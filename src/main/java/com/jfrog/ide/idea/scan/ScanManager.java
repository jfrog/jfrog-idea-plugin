package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.common.tree.Artifact;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.common.tree.ImpactTreeNode;
import com.jfrog.ide.common.tree.Issue;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import com.jfrog.xray.client.services.summary.Components;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jfrog.ide.common.log.Utils.logError;

/**
 * Created by romang on 4/26/17.
 */
public abstract class ScanManager {
    private ServerConfig serverConfig;
    private ComponentPrefix prefix;
    private ScanLogic scanLogic;
    private String projectName;
    private Log log;
    private ExecutorService executor;
    protected Project project;
    String basePath;

    // Lock to prevent multiple simultaneous scans
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    /**
     * @param project  - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                 like {@link ConsistentFilterManager} and {@link ComponentsTree}
     * @param basePath - Project base path
     * @param prefix   - Components prefix for xray scan, e.g. gav:// or npm://
     * @param executor - An executor that should limit the number of running tasks to 3
     */
    ScanManager(@NotNull Project project, String basePath, ComponentPrefix prefix, ExecutorService executor) {
        this.serverConfig = GlobalSettings.getInstance().getServerConfig();
        this.projectName = basePath;
        this.prefix = prefix;
        this.log = Logger.getInstance();
        this.executor = executor;
        this.basePath = basePath;
        this.project = project;
    }

    void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    // TODO: arguments were changed?
    /**
     * Collect and return {@link Components} to be scanned by JFrog Xray.
     * Implementation should be project type specific.
     */
    protected abstract DependencyTree buildTree() throws IOException;

    /**
     * Return all project descriptors under the scan-manager project, which need to be inspected by the corresponding {@link LocalInspectionTool}.
     *
     * @return all project descriptors under the scan-manager project to be inspected.
     */
    protected abstract PsiFile[] getProjectDescriptors();

    /**
     * Return the Inspection tool corresponding to the scan-manager type.
     * The returned Inspection tool is used to perform the inspection on the project-descriptor files.
     *
     * @return the Inspection tool corresponding to the scan-manager type.
     */
    protected abstract AbstractInspection getInspectionTool();

    protected void sendUsageReport() {
        Utils.sendUsageReport(getProjectPackageType() + "-deps");
    }

    protected abstract String getProjectPackageType();

    protected abstract List<FileTreeNode> groupArtifactsToDescriptorNodes(Collection<Artifact> depScanResults, Map<String, List<DependencyTree>> depMap);

    public abstract String getPackageType();

    // TODO: shouldToast was removed in master!!!!
    /**
     * Scan and update dependency components.
     *
     * @param indicator   - The progress indicator
     */
    private void scanAndUpdate(ProgressIndicator indicator) {
        try {
            indicator.setText("1/3: Building dependency tree");
            DependencyTree dependencyTree = buildTree();
            indicator.setText("2/3: Xray scanning project dependencies");
            log.debug("Start scan for '" + projectName + "'.");
            Map<String, Artifact> results = scanLogic.scanArtifacts(dependencyTree, serverConfig, indicator, prefix, this::checkCanceled);
            indicator.setText("3/3: Finalizing");
            if (results == null) {
                log.debug("Wasn't able to scan '" + projectName + "'.");
                return;
            }
            Map<String, List<DependencyTree>> depMap = new HashMap<>();
            mapDependencyTree(depMap, dependencyTree);

            Map<String, List<Issue>> issuesMap = mapIssuesByCve(results);

            createImpactPaths(results, depMap, dependencyTree);
            // TODO: convert results to tree, and save it to cache!
            List<FileTreeNode> fileTreeNodes = groupArtifactsToDescriptorNodes(results.values(), depMap);
            addScanResults(fileTreeNodes);

            // TODO: uncomment
//            DumbService.getInstance(project).smartInvokeLater(this::runInspections);
        } catch (ProcessCanceledException e) {
            log.info("Xray scan was canceled");
        } catch (Exception e) {
            logError(log, "Xray Scan failed", e, true);
        } finally {
            scanInProgress.set(false);
            sendUsageReport();
        }
    }

    private void mapDependencyTree(Map<String, List<DependencyTree>> depMap, DependencyTree root) {
        if (!depMap.containsKey(root.getComponentId())) {
            depMap.put(root.getComponentId(), new ArrayList<>());
        }
        depMap.get(root.getComponentId()).add(root);
        for (DependencyTree child : root.getChildren()) {
            mapDependencyTree(depMap, child);
        }
    }

    /**
     * Maps all the issues (vulnerabilities and security violations) by their CVE IDs.
     * Issues without a CVE ID are ignored.
     *
     * @param results - scan results mapped by dependencies.
     * @return a map of CVE IDs to lists of issues with them.
     */
    private Map<String, List<Issue>> mapIssuesByCve(Map<String, Artifact> results) {
        Map<String, List<Issue>> issues = new HashMap<>();
        for (Artifact dep : results.values()) {
            for (TreeNode node : Collections.list(dep.children())) {
                if (!(node instanceof Issue)) {
                    continue;
                }
                Issue issue = (Issue) node;
                if (issue.getCve() == null || issue.getCve().getCveId() == null || issue.getCve().getCveId().isEmpty()) {
                    continue;
                }
                if (!issues.containsKey(issue.getCve().getCveId())) {
                    issues.put(issue.getCve().getCveId(), new ArrayList<>());
                }
                issues.get(issue.getCve().getCveId()).add(issue);
            }
        }
        return issues;
    }

    private void createImpactPaths(Map<String, Artifact> dependencies, Map<String, List<DependencyTree>> depMap, DependencyTree root) {
        for (Map.Entry<String, Artifact> depEntry : dependencies.entrySet()) {
            Map<DependencyTree, ImpactTreeNode> impactTreeNodes = new HashMap<>();
            for (DependencyTree depTree : depMap.get(depEntry.getKey())) {
                addImpactPath(impactTreeNodes, depTree);
            }
            depEntry.getValue().setImpactPaths(impactTreeNodes.get(root));
        }
    }

    private ImpactTreeNode addImpactPath(Map<DependencyTree, ImpactTreeNode> impactTreeNodes, DependencyTree depTreeNode) {
        if (impactTreeNodes.containsKey(depTreeNode)) {
            return impactTreeNodes.get(depTreeNode);
        }
        ImpactTreeNode parentImpactTreeNode = null;
        if (depTreeNode.getParent() != null) {
            parentImpactTreeNode = addImpactPath(impactTreeNodes, (DependencyTree) depTreeNode.getParent());
        }
        ImpactTreeNode currImpactTreeNode = new ImpactTreeNode(depTreeNode.getComponentId());
        if (parentImpactTreeNode != null) {
            parentImpactTreeNode.getChildren().add(currImpactTreeNode);
        }
        impactTreeNodes.put(depTreeNode, currImpactTreeNode);
        return currImpactTreeNode;
    }

    /**
     * Launch async dependency scan.
     */
    void asyncScanAndUpdateResults() {
            if (DumbService.isDumb(project)) { // If intellij is still indexing the project
            return;
        }
        // The tasks run asynchronously. To make sure no more than 3 tasks are running concurrently,
        // we use a count down latch that signals to that executor service that it can get more tasks.
        CountDownLatch latch = new CountDownLatch(1);
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(null, getTaskTitle()) {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                if (project.isDisposed()) {
                    return;
                }
                if (!GlobalSettings.getInstance().areXrayCredentialsSet()) { // TODO: shouldn't this check be before the scan?
                    log.warn("Xray server is not configured.");
                    return;
                }
                // Prevent multiple simultaneous scans
                if (!scanInProgress.compareAndSet(false, true)) {
                    log.info("Scan already in progress");
                    return;
                }
                scanAndUpdate(new ProgressIndicatorImpl(indicator));
            }

            @Override
            public void onFinished() {
                latch.countDown();
            }
        };
        if (executor.isShutdown() || executor.isTerminated()) {
            // Scan initiated by a change in the project descriptor
            createRunnable(scanAndUpdateTask, null).run();
        } else {
            // Scan initiated by opening IntelliJ, by user, or by changing the configuration
            executor.submit(createRunnable(scanAndUpdateTask, latch));
        }
    }

    /**
     * Get text to display in the task progress.
     *
     * @return text to display in the task progress.
     */
    private String getTaskTitle() {
        Path projectBasePath = Utils.getProjectBasePath(project);
        Path wsBasePath = Paths.get(basePath);
        String relativePath = "";
        if (projectBasePath.isAbsolute() == wsBasePath.isAbsolute()) {
            // If one of the path is relative and the other one is absolute, the following exception is thrown:
            // IllegalArgumentException: 'other' is different type of Path
            relativePath = projectBasePath.relativize(wsBasePath).toString();
        }
        return "JFrog Xray scanning " + StringUtils.defaultIfBlank(relativePath, project.getName());
    }

    /**
     * Create a runnable to be submitted to the executor service, or run directly.
     *
     * @param scanAndUpdateTask - The task to submit
     * @param latch             - The countdown latch, which makes sure the executor service doesn't get more than 3 tasks.
     *                          If null, the scan was initiated by a change in the project descriptor and the executor
     *                          service is terminated. In this case, there is no requirement to wait.
     */
    private Runnable createRunnable(Task.Backgroundable scanAndUpdateTask, CountDownLatch latch) {
        return () -> {
            // The progress manager is only good for foreground threads.
            if (SwingUtilities.isEventDispatchThread()) {
                scanAndUpdateTask.queue();
            } else {
                // Run the scan task when the thread is in the foreground.
                ApplicationManager.getApplication().invokeLater(scanAndUpdateTask::queue);
            }
            try {
                // Wait for scan to finish, to make sure the thread pool remain full
                if (latch != null) {
                    latch.await();
                }
            } catch (InterruptedException e) {
                logError(log, ExceptionUtils.getRootCauseMessage(e), e, true);
            }
        };
    }

    /**
     * Returns all project modules locations as Paths.
     * Other scanners such as npm will use these paths in order to find modules.
     *
     * @return all project modules locations as Paths
     */
    public Set<Path> getProjectPaths() {
        Set<Path> paths = Sets.newHashSet();
        paths.add(Paths.get(basePath));
        return paths;
    }

    void runInspections() {
        PsiFile[] projectDescriptors = getProjectDescriptors();
        if (ArrayUtils.isEmpty(projectDescriptors)) {
            return;
        }
        InspectionManagerEx inspectionManagerEx = (InspectionManagerEx) InspectionManager.getInstance(project);
        GlobalInspectionContext context = inspectionManagerEx.createNewGlobalContext(false);
        AbstractInspection localInspectionTool = getInspectionTool();
        localInspectionTool.setAfterScan(true);
        for (PsiFile descriptor : projectDescriptors) {
            // Run inspection on descriptor.
            try {
                InspectionEngine.runInspectionOnFile(descriptor, new LocalInspectionToolWrapper(localInspectionTool), context);
                // TODO: remove the try/catch
            } catch (Exception e) {
                Logger.getInstance().error("Inspection failed", e);
            }
            FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors(descriptor.getVirtualFile());
            if (!ArrayUtils.isEmpty(editors)) {
                // Refresh descriptor highlighting only if it is already opened.
                DaemonCodeAnalyzer.getInstance(project).restart(descriptor);
            }
        }
    }

    /**
     * filter scan components tree model according to the user filters and sort the issues tree.
     */
    private void addScanResults(List<FileTreeNode> fileTreeNodes) {
        // TODO: make sure that it's null, if there are no violations
        if (fileTreeNodes.isEmpty()) {
            return;
        }
        LocalComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        componentsTree.addScanResults(fileTreeNodes);
    }

    protected void checkCanceled() {
        if (project.isOpen()) {
            // The project is closed if we are in test mode.
            // In tests, we can't check if the user canceled the scan, since we don't have the ProgressManager service.
            ProgressManager.checkCanceled();
        }
    }

    boolean isScanInProgress() {
        return this.scanInProgress.get();
    }

    public String getProjectPath() {
        return this.basePath;
    }

    public void setScanLogic(ScanLogic scanLogic) {
        this.scanLogic = scanLogic;
    }

    public String getProjectName() {
        return projectName;
    }

    public Log getLog() {
        return log;
    }
}
