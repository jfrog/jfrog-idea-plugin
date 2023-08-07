package com.jfrog.ide.idea.scan;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.*;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.idea.scan.ScannerBase.createRunnable;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.*;
import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;

public class SourceCodeScannerManager {
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private final ApplicabilityScannerExecutor applicability = new ApplicabilityScannerExecutor(Logger.getInstance(), GlobalSettings.getInstance().getServerConfig());

    private final Collection<ScanBinaryExecutor> scanners = initScannersCollection();

    protected Project project;
    protected PackageManagerType packageType;
    private static final String SKIP_FOLDERS_SUFFIX = "*/**";

    public SourceCodeScannerManager(Project project) {
        this.project = project;
    }

    public SourceCodeScannerManager(Project project, PackageManagerType packageType) {
        this(project);
        this.packageType = packageType;
    }

    /**
     * Applicability source code scanning (Contextual Analysis).
     *
     * @param indicator     the progress indicator.
     * @param fileTreeNodes collection of FileTreeNodes.
     * @return A list of FileTreeNodes having the source code issues as their children.
     */
    public List<FileTreeNode> applicabilityScan(ProgressIndicator indicator, Collection<FileTreeNode> fileTreeNodes, Runnable checkCanceled) {
        if (project.isDisposed()) {
            return Collections.emptyList();
        }
        // Prevent multiple simultaneous scans
        if (!scanInProgress.compareAndSet(false, true)) {
            return Collections.emptyList();
        }
        List<JFrogSecurityWarning> scanResults = new ArrayList<>();
        Map<String, List<VulnerabilityNode>> issuesMap = mapDirectIssuesByCve(fileTreeNodes);

        try {
            if (applicability.isPackageTypeSupported(packageType)) {
                indicator.setText("Running applicability scan");
                indicator.setFraction(0.25);
                Set<String> directIssuesCVEs = issuesMap.keySet();
                // If no direct dependencies with issues are found by Xray, the applicability scan is irrelevant.
                if (!directIssuesCVEs.isEmpty()) {
                    List<JFrogSecurityWarning> applicabilityResults = applicability.execute(createBasicScannerInput().cves(List.copyOf(directIssuesCVEs)), checkCanceled);
                    scanResults.addAll(applicabilityResults);
                }
            }
        } catch (IOException | InterruptedException | NullPointerException e) {
            logError(Logger.getInstance(), "Failed to scan source code", e, true);
        } finally {
            scanInProgress.set(false);
            indicator.setFraction(1);
        }
        return createAndUpdateApplicabilityIssueNodes(scanResults, issuesMap);
    }

    /**
     * Launch async source code scans.
     */
    void asyncScanAndUpdateResults(ExecutorService executor, Logger log) {
        // If intellij is still indexing the project, do not scan.
        if (DumbService.isDumb(project)) {
            return;
        }
        // The tasks run asynchronously. To make sure no more than 3 tasks are running concurrently,
        // we use a count-down latch that signals to that executor service that it can get more tasks.
        CountDownLatch latch = new CountDownLatch(1);
        Task.Backgroundable sourceCodeScanTask = new Task.Backgroundable(null, "Advanced source code scanning") {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                if (project.isDisposed()) {
                    return;
                }
                if (!GlobalSettings.getInstance().reloadXrayCredentials()) {
                    throw new RuntimeException("Xray server is not configured.");
                }
                // Prevent multiple simultaneous scans
                if (!scanInProgress.compareAndSet(false, true)) {
                    log.info("Advanced source code scan is already in progress");
                    return;
                }
                sourceCodeScanAndUpdate(new ProgressIndicatorImpl(indicator), ProgressManager::checkCanceled, log);
            }

            @Override
            public void onFinished() {
                latch.countDown();
                scanInProgress.set(false);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                log.error(ExceptionUtils.getRootCauseMessage(error));
            }

        };
        executor.submit(createRunnable(sourceCodeScanTask, latch, log));
    }

    private void sourceCodeScanAndUpdate(ProgressIndicator indicator, Runnable checkCanceled, Logger log) {
        indicator.setText("Running advanced source code scanning");
        double fraction = 0;
        for (ScanBinaryExecutor scanner : scanners) {
            checkCanceled.run();
            try {
                List<JFrogSecurityWarning> scanResults = scanner.execute(createBasicScannerInput(), checkCanceled);
                addSourceCodeScanResults(createFileIssueNodes(scanResults));
            } catch (IOException | URISyntaxException | InterruptedException e) {
                logError(log, "", e, true);
            }
            fraction += 1.0 / scanners.size();
            indicator.setFraction(fraction);
        }
    }

    private void addSourceCodeScanResults(List<FileTreeNode> fileTreeNodes) {
        if (fileTreeNodes.isEmpty()) {
            return;
        }
        LocalComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        componentsTree.addScanResults(fileTreeNodes);
    }

    private ScanConfig.Builder createBasicScannerInput() {
        String excludePattern = GlobalSettings.getInstance().getServerConfig().getExcludedPaths();
        return new ScanConfig.Builder().roots(List.of(getProjectBasePath(project).toString())).skippedFolders(convertToSkippedFolders(excludePattern));
    }

    /**
     * Splits the users' configured ExcludedPaths glob pattern to a list
     * of simplified patterns by avoiding the use of "{}".
     *
     * @return a list of equivalent patterns without the use of "{}"
     */
    static List<String> convertToSkippedFolders(String excludePattern) {
        List<String> skippedFoldersPatterns = new ArrayList<>();
        if (StringUtils.isNotBlank(excludePattern)) {
            Matcher matcher = EXCLUSIONS_REGEX_PATTERN.matcher(excludePattern);
            if (!matcher.find()) {
                // Convert pattern form shape "**/*a*" to "**/*a*/**"
                return List.of(StringUtils.removeEnd(excludePattern, EXCLUSIONS_SUFFIX) + SKIP_FOLDERS_SUFFIX);
            }
            String[] dirsNames = matcher.group(1).split(",");
            for (String dirName : dirsNames) {
                skippedFoldersPatterns.add(EXCLUSIONS_PREFIX + dirName.strip() + SKIP_FOLDERS_SUFFIX);
            }
        }
        return skippedFoldersPatterns;
    }

    /**
     * Create {@link FileTreeNode}s with file issues nodes.
     *
     * @param scanResults a list of source code scan results.
     * @return a list of new {@link FileTreeNode}s containing source code issues.
     */
    private List<FileTreeNode> createFileIssueNodes(List<JFrogSecurityWarning> scanResults) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : scanResults) {
            // Create FileTreeNodes for files with found issues
            FileTreeNode fileNode = results.get(warning.getFilePath());
            if (fileNode == null) {
                fileNode = new FileTreeNode(warning.getFilePath());
                results.put(warning.getFilePath(), fileNode);
            }

            FileIssueNode issueNode = new FileIssueNode(createTitle(warning),
                    warning.getFilePath(), warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                    createReason(warning), warning.getLineSnippet(), warning.getReporter(), warning.getSeverity());
            fileNode.addIssue(issueNode);
        }
        return new ArrayList<>(results.values());
    }

    private String createReason(JFrogSecurityWarning warning) {
        return switch (warning.getReporter()) {
            case IAC, SECRETS -> warning.getScannerSearchTarget();
            default -> warning.getReason();
        };
    }

    private String createTitle(JFrogSecurityWarning warning) {
        return switch (warning.getReporter()) {
            case SECRETS -> "Potential Secret";
            case IAC -> "Infrastructure as Code Vulnerability";
            default -> warning.getName();
        };
    }

    /**
     * Create {@link FileTreeNode}s with applicability issues and update applicability issues in {@link VulnerabilityNode}s.
     *
     * @param scanResults a list of source code scan results.
     * @param issuesMap   a map of {@link VulnerabilityNode}s mapped by their CVEs.
     * @return a list of new {@link FileTreeNode}s containing source code issues.
     */
    private List<FileTreeNode> createAndUpdateApplicabilityIssueNodes(List<JFrogSecurityWarning> scanResults, Map<String, List<VulnerabilityNode>> issuesMap) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : scanResults) {
            // Update all VulnerabilityNodes that have the warning's CVE
            String cve = StringUtils.removeStart(warning.getName(), "applic_");
            List<VulnerabilityNode> issues = issuesMap.get(cve);
            if (issues != null) {
                if (warning.isApplicable()) {
                    // Create FileTreeNodes for files with applicable issues
                    FileTreeNode fileNode = results.get(warning.getFilePath());
                    if (fileNode == null) {
                        fileNode = new FileTreeNode(warning.getFilePath());
                        results.put(warning.getFilePath(), fileNode);
                    }

                    ApplicableIssueNode applicableIssue = new ApplicableIssueNode(
                            cve, warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                            warning.getFilePath(), warning.getReason(), warning.getLineSnippet(), warning.getScannerSearchTarget(),
                            issues.get(0));
                    fileNode.addIssue(applicableIssue);
                    for (VulnerabilityNode issue : issues) {
                        issue.updateApplicableInfo(applicableIssue);
                    }
                } else {
                    // Mark non-applicable vulnerabilities.
                    for (VulnerabilityNode issue : issues) {
                        issue.setNotApplicable();
                    }
                }
            }
        }
        return new ArrayList<>(results.values());
    }

    /**
     * Maps direct dependencies  issues (vulnerabilities and security violations) by their CVE IDs.
     * Issues without a CVE ID are ignored.
     *
     * @param fileTreeNodes - collection of FileTreeNodes.
     * @return a map of CVE IDs to lists of issues with them.
     */
    private Map<String, List<VulnerabilityNode>> mapDirectIssuesByCve(Collection<FileTreeNode> fileTreeNodes) {
        Map<String, List<VulnerabilityNode>> issues = new HashMap<>();
        for (FileTreeNode fileTreeNode : fileTreeNodes) {
            for (TreeNode treeNode : fileTreeNode.getChildren()) {
                DependencyNode dep = (DependencyNode) treeNode;
                if (dep.isIndirect()) {
                    continue;
                }
                Enumeration<TreeNode> treeNodeEnumeration = dep.children();
                while (treeNodeEnumeration.hasMoreElements()) {
                    TreeNode node = treeNodeEnumeration.nextElement();
                    if (!(node instanceof VulnerabilityNode vulnerabilityNode)) {
                        continue;
                    }
                    if (vulnerabilityNode.getCve() == null || StringUtils.isBlank(vulnerabilityNode.getCve().getCveId())) {
                        continue;
                    }
                    String cveId = vulnerabilityNode.getCve().getCveId();
                    issues.putIfAbsent(cveId, new ArrayList<>());
                    issues.get(cveId).add(vulnerabilityNode);
                }
            }
        }
        return issues;
    }

    private Collection<ScanBinaryExecutor> initScannersCollection() {
        return List.of(
                new SecretsScannerExecutor(Logger.getInstance(), GlobalSettings.getInstance().getServerConfig()),
                new IACScannerExecutor(Logger.getInstance(), GlobalSettings.getInstance().getServerConfig())
        );
    }
}
