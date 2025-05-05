package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.VulnerabilityNode;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.applications.JFrogApplicationsConfig;
import com.jfrog.ide.idea.scan.data.applications.ModuleConfig;
import com.jfrog.ide.idea.scan.data.applications.ScannerConfig;
import com.jfrog.ide.idea.scan.utils.SourceScanType;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;
import static com.jfrog.ide.idea.scan.ScannerBase.createRunnable;
import static com.jfrog.ide.idea.scan.data.applications.JFrogApplicationsConfig.createApplicationConfigWithDefaultModule;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.EXCLUSIONS_PREFIX;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.EXCLUSIONS_REGEX_PATTERN;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.EXCLUSIONS_SUFFIX;
import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class SourceCodeScannerManager {
    private final Path jfrogApplictionsConfigPath;
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private final ApplicabilityScannerExecutor applicability = new ApplicabilityScannerExecutor(Logger.getInstance());
    private final Map<SourceScanType, ScanBinaryExecutor> scanners = initScannersCollection();
    protected Project project;
    protected PackageManagerType packageType;
    private static final String SKIP_FOLDERS_SUFFIX = "*/**";
    private com.intellij.openapi.progress.ProgressIndicator progressIndicator;

    public SourceCodeScannerManager(Project project) {
        this.project = project;
        this.jfrogApplictionsConfigPath = getProjectBasePath(project).resolve(".jfrog").resolve("jfrog-apps-config.yml");
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
                    List<JFrogSecurityWarning> applicabilityResults = applicability.execute(createBasicScannerInput().cves(List.copyOf(directIssuesCVEs)), checkCanceled, indicator);
                    scanResults.addAll(applicabilityResults);
                }
            }
        } catch (InterruptedException e) {
            logError(Logger.getInstance(), "Scan canceled due to a user request or timeout.", false);
        } catch (IOException | NullPointerException e) {
            logError(Logger.getInstance(), "Failed to scan source code", e, true);
        } finally {
            scanInProgress.set(false);
            indicator.setFraction(1);
        }
        return applicability.createSpecificFileIssueNodes(scanResults, issuesMap);
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
                // Prevent multiple simultaneous scans
                if (!scanInProgress.compareAndSet(false, true)) {
                    log.info("Advanced source code scan is already in progress");
                    return;
                }
                try {
                    progressIndicator = indicator;
                    sourceCodeScanAndUpdate(new ProgressIndicatorImpl(indicator), ProgressManager::checkCanceled, log);
                } catch (IOException e) {
                    logError(Logger.getInstance(), "Failed to run advanced source code scanning.", e, true);
                }
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
        executor.submit(createRunnable(sourceCodeScanTask, latch, progressIndicator, log));
    }

    public void stopScan() {
        if (progressIndicator != null) {
            progressIndicator.cancel();
        }
    }

    private void sourceCodeScanAndUpdate(ProgressIndicator indicator, Runnable checkCanceled, Logger log) throws IOException {
        indicator.setText("Running advanced source code scanning");
        JFrogApplicationsConfig projectConfig = parseJFrogApplicationsConfig();

        for (ModuleConfig moduleConfig : projectConfig.getModules()) {
            scan(moduleConfig, indicator, checkCanceled, log);
        }
    }

    private void scan(ModuleConfig moduleConfig, ProgressIndicator indicator, Runnable checkCanceled, Logger log) {
        double fraction = 0;
        for (SourceScanType scannerType : scanners.keySet()) {
            checkCanceled.run();
            ScanBinaryExecutor scanner = scanners.get(scannerType);
            ScannerConfig scannerConfig = null;
            if (moduleConfig != null) {
                // Skip the scanner If requested.
                if (moduleConfig.getExcludeScanners() != null && moduleConfig.getExcludeScanners().contains(scannerType.toString().toLowerCase())) {
                    log.debug(String.format("Skipping %s scanning", scannerType.toString().toLowerCase()));
                    continue;
                }
                // Use specific scanner config if exists.
                if (moduleConfig.getScanners() != null) {
                    scannerConfig = moduleConfig.getScanners().get(scannerType.toString().toLowerCase());
                }
            }
            try {
                List<JFrogSecurityWarning> scanResults = scanner.execute(createBasicScannerInput(moduleConfig, scannerConfig), checkCanceled, indicator);
                addSourceCodeScanResults(scanner.createSpecificFileIssueNodes(scanResults));
            } catch (IOException | URISyntaxException | InterruptedException e) {
                logError(log, "", e, true);
            }
            fraction += 1.0 / scanners.size();
            indicator.setFraction(fraction);
        }
    }

    private JFrogApplicationsConfig parseJFrogApplicationsConfig() throws IOException {
        ObjectMapper mapper = createYAMLMapper();
        File config = jfrogApplictionsConfigPath.toFile();
        if (config.exists()) {
            return mapper.readValue(config, JFrogApplicationsConfig.class);
        }
        return createApplicationConfigWithDefaultModule(project);
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
        return new ScanConfig.Builder().roots(List.of(getProjectBasePath(project).toAbsolutePath().toString())).skippedFolders(convertToSkippedFolders(excludePattern));
    }

    private ScanConfig.Builder createBasicScannerInput(ModuleConfig config, ScannerConfig scannerConfig) {
        if (config == null) {
            return createBasicScannerInput();
        }

        // Scanner's working dirs (roots)
        List<String> workingDirs = new ArrayList<>();
        String projectBasePath = defaultIfEmpty(config.getSourceRoot(), getProjectBasePath(project).toAbsolutePath().toString());
        if (scannerConfig != null && !CollectionUtils.isEmpty(scannerConfig.getWorkingDirs())) {
            for (String workingDir : scannerConfig.getWorkingDirs()) {
                workingDirs.add(Paths.get(projectBasePath).resolve(workingDir).toString());
            }
        } else {
            // Default: ".", the application's root directory.
            workingDirs.add(projectBasePath);
        }

        // Module exclude patterns
        List<String> skippedFolders = new ArrayList<>();
        if (config.getExcludePatterns() != null) {
            skippedFolders.addAll(config.getExcludePatterns());
        }
        if (scannerConfig != null && scannerConfig.getExcludePatterns() != null) {
            // Adds scanner specific exclude patterns if exists
            skippedFolders.addAll(scannerConfig.getExcludePatterns());
        }
        String excludePattern = GlobalSettings.getInstance().getServerConfig().getExcludedPaths();
        // If exclude patterns was not provided, use the configured IDE patterns.
        skippedFolders = skippedFolders.isEmpty() ? convertToSkippedFolders(excludePattern) : skippedFolders;

        // Extra scanners params
        List<String> excludeRules = null;
        String language = null;
        if (scannerConfig != null) {
            excludeRules = scannerConfig.getExcludedRules();
            language = scannerConfig.getLanguage();
        }

        return new ScanConfig.Builder().roots(workingDirs).skippedFolders(skippedFolders).excludedRules(excludeRules).language(language);
    }

    /**
     * Splits the users' configured ExcludedPaths glob pattern to a list
     * of simplified patterns by avoiding the use of "{}".
     *
     * @return a list of equivalent patterns without the use of "{}"
     */
    public static List<String> convertToSkippedFolders(String excludePattern) {
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
     * Maps direct dependencies  issues (vulnerabilities and security violations) by their CVE IDs.
     * Issues without a CVE ID are ignored.
     *
     * @param fileTreeNodes collection of FileTreeNodes.
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

    private Map<SourceScanType, ScanBinaryExecutor> initScannersCollection() {
        Map<SourceScanType, ScanBinaryExecutor> scanners = new HashMap<>();
        scanners.put(SourceScanType.SECRETS, new SecretsScannerExecutor(Logger.getInstance()));
        scanners.put(SourceScanType.IAC, new IACScannerExecutor(Logger.getInstance()));
        scanners.put(SourceScanType.SAST, new SastScannerExecutor(Logger.getInstance()));
        return scanners;
    }

    public boolean isScanInProgress() {
        return this.scanInProgress.get();
    }
}
