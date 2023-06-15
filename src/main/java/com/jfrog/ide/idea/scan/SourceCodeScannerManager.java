package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.*;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.PackageType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.lang.StringUtils;

import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.*;
import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;

public class SourceCodeScannerManager {
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private final ApplicabilityScannerExecutor applicability = new ApplicabilityScannerExecutor(Logger.getInstance(), GlobalSettings.getInstance().getServerConfig());

    private final Collection<ScanBinaryExecutor> scanners = initScannersCollection();

    protected Project project;
    protected PackageType packageType;
    private static final String SKIP_FOLDERS_SUFFIX = "*/**";

    public SourceCodeScannerManager(Project project, PackageType packageType) {
        this.project = project;
        this.packageType = packageType;
    }

    /**
     * Applicability source code scanning (Contextual Analysis).
     *
     * @param indicator      the progress indicator.
     * @param depScanResults collection of DependencyNodes.
     * @return A list of FileTreeNodes having the source code issues as their children.
     */
    public List<FileTreeNode> applicabilityScan(ProgressIndicator indicator, Collection<DependencyNode> depScanResults, Runnable checkCanceled) {
        if (project.isDisposed()) {
            return Collections.emptyList();
        }
        // Prevent multiple simultaneous scans
        if (!scanInProgress.compareAndSet(false, true)) {
            return Collections.emptyList();
        }
        List<JFrogSecurityWarning> scanResults = new ArrayList<>();
        Map<String, List<VulnerabilityNode>> issuesMap = mapDirectIssuesByCve(depScanResults);

        try {
            if (applicability.isPackageTypeSupported(packageType)) {
                indicator.setText("Running applicability scan");
                indicator.setFraction(0.25);
                Set<String> directIssuesCVEs = issuesMap.keySet();
                // If no direct dependencies with issues are found by Xray, the applicability scan is irrelevant.
                if (directIssuesCVEs.size() > 0) {
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

    public List<FileTreeNode> sourceCodeScan(ProgressIndicator indicator, Runnable checkCanceled) throws IOException, URISyntaxException, InterruptedException {
        List<JFrogSecurityWarning> results = new ArrayList<>();
        indicator.setText("Running advance source code scanning");
        double fraction = 0;
        for (var scanner : scanners) {
            checkCanceled.run();
            results.addAll(scanner.execute(createBasicScannerInput(), checkCanceled));
            fraction += 1.0 / scanners.size();
            indicator.setFraction(fraction);
        }
        return createFileIssueNodes(results);
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

            FileIssueNode issueNode = new FileIssueNode(warning.getName(),
                    warning.getFilePath(), warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                    warning.getReason(), warning.getLineSnippet(), warning.getReporter());
            fileNode.addIssue(issueNode);
        }
        return new ArrayList<>(results.values());
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
                    //noinspection DataFlowIssue
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
     * @param depScanResults - collection of DependencyNodes.
     * @return a map of CVE IDs to lists of issues with them.
     */
    private Map<String, List<VulnerabilityNode>> mapDirectIssuesByCve(Collection<DependencyNode> depScanResults) {
        Map<String, List<VulnerabilityNode>> issues = new HashMap<>();
        for (DependencyNode dep : depScanResults) {
            if (dep.isIndirect()) {
                continue;
            }
            Enumeration<TreeNode> treeNodeEnumeration = dep.children();
            while (treeNodeEnumeration.hasMoreElements()) {
                TreeNode node = treeNodeEnumeration.nextElement();
                if (!(node instanceof VulnerabilityNode)) {
                    continue;
                }
                VulnerabilityNode vulnerabilityNode = (VulnerabilityNode) node;
                String cveId = vulnerabilityNode.getCve().getCveId();
                if (vulnerabilityNode.getCve() == null || StringUtils.isBlank(cveId)) {
                    continue;
                }
                issues.putIfAbsent(cveId, new ArrayList<>());
                issues.get(cveId).add(vulnerabilityNode);
            }
        }
        return issues;
    }

    private Collection<ScanBinaryExecutor> initScannersCollection() {
        return List.of(new SecretsScannerExecutor(Logger.getInstance(), GlobalSettings.getInstance().getServerConfig()));
    }
}
