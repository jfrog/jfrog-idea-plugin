package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
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
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTree;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import com.jfrog.xray.client.services.summary.Components;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.util.Log;

import javax.annotation.Nullable;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jfrog.ide.common.log.Utils.logError;

/**
 * Created by romang on 4/26/17.
 */
public abstract class ScannerBase {
    public static final int IMPACT_PATHS_LIMIT = 50;

    private final ServerConfig serverConfig;
    private final ComponentPrefix prefix;
    @Getter
    private final Log log;
    // Lock to prevent multiple simultaneous scans
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private final AtomicBoolean scanError = new AtomicBoolean(false);
    private final AtomicBoolean scanCanceled = new AtomicBoolean(false);

    private ScanLogic scanLogic;
    protected Project project;
    protected SourceCodeScannerManager sourceCodeScannerManager;
    String basePath;
    private ExecutorService executor;
    private com.intellij.openapi.progress.ProgressIndicator progressIndicator;

    /**
     * @param project   currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                  like {@link ConsistentFilterManager} and {@link ComponentsTree}
     * @param basePath  project base path
     * @param prefix    components prefix for xray scan, e.g. gav:// or npm://
     * @param executor  an executor that should limit the number of running tasks to 3
     * @param scanLogic the scan logic to use
     */
    ScannerBase(@NotNull Project project, String basePath, ComponentPrefix prefix, ExecutorService executor, ScanLogic scanLogic) {
        this.serverConfig = GlobalSettings.getInstance().getServerConfig();
        this.prefix = prefix;
        this.log = Logger.getInstance();
        this.executor = executor;
        this.basePath = basePath;
        this.project = project;
        this.sourceCodeScannerManager = new SourceCodeScannerManager(project, getPackageManagerType());
        this.scanLogic = scanLogic;
    }

    void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    void setScanLogic(ScanLogic logic) {
        this.scanLogic = logic;
    }

    /**
     * Collect and return {@link Components} to be scanned by JFrog Xray.
     * Implementation should be project type specific.
     */
    protected abstract DepTree buildTree() throws IOException;

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
    @Nullable
    protected abstract AbstractInspection getInspectionTool();

    protected void sendUsageReport() {
        ApplicationManager.getApplication().invokeLater(() -> Utils.sendUsageReport(getPackageManagerType().getName() + "-deps"));
    }

    protected abstract PackageManagerType getPackageManagerType();

    /**
     * Scan and update dependency components.
     *
     * @param indicator - The progress indicator
     */
    private void scanAndUpdate(ProgressIndicator indicator) {
        try {
            sendUsageReport();

            // Building dependency tree
            indicator.setText("1/3: Building dependency tree");
            DepTree depTree = buildTree();
            checkCanceled();

            // Sending the dependency tree to Xray for scanning
            indicator.setText("2/3: Xray scanning project dependencies");
            log.debug("Start scan for '" + basePath + "'.");
            Map<String, DependencyNode> results = scanLogic.scanArtifacts(depTree, serverConfig, indicator, prefix, this::checkCanceled);
            checkCanceled();

            indicator.setText("3/3: Finalizing");
            if (results == null || results.isEmpty()) {
                // No violations/vulnerabilities or no components to scan or an error was thrown
                return;
            }
            List<FileTreeNode> fileTreeNodes = walkDepTree(results, depTree);
            addScanResults(fileTreeNodes);

            // Contextual Analysis
            List<FileTreeNode> applicabilityScanResults = sourceCodeScannerManager.applicabilityScan(indicator, fileTreeNodes, this::checkCanceled);
            addScanResults(applicabilityScanResults);

            // Force inspections run due to changes in the displayed tree
            runInspections();

        } catch (ProcessCanceledException e) {
            log.info("Xray scan was canceled");
            scanCanceled.set(true);
        } catch (Exception e) {
            scanError.set(true);
            logError(log, "Xray scan failed", e, true);
        }
    }

    /**
     * Walks through a {@link DepTree}'s nodes.
     * Builds impact paths for {@link DependencyNode} objects and groups them in {@link DescriptorFileTreeNode}s.
     *
     * @param vulnerableDependencies a map of component IDs and the DependencyNode object matching each of them.
     * @param depTree                the project's dependency tree to walk through.
     */
    protected List<FileTreeNode> walkDepTree(Map<String, DependencyNode> vulnerableDependencies, DepTree depTree) throws IOException {
        Map<String, DescriptorFileTreeNode> descriptorNodes = new HashMap<>();
        visitDepTreeNode(vulnerableDependencies, depTree, Collections.singletonList(depTree.getRootId()), descriptorNodes, new ArrayList<>(), new HashMap<>());
        return new CopyOnWriteArrayList<>(descriptorNodes.values());
    }

    /**
     * Visit a node in the {@link DepTree} and walk through its children.
     * Each impact path to a vulnerable dependency is added in its {@link DependencyNode}.
     * Each DependencyNode is added to the relevant {@link DescriptorFileTreeNode}s.
     *
     * @param vulnerableDependencies a map of {@link DependencyNode}s by their component IDs.
     * @param depTree                the project's dependency tree.
     * @param path                   a path of nodes (represented by their component IDs) from the root to the current node.
     * @param descriptorNodes        a map of {@link DescriptorFileTreeNode}s by the descriptor file path. Missing DescriptorFileTreeNodes will be added to this map.
     * @param descriptorPaths        a list of descriptor file paths that their matching components are in the path to the current node.
     * @param addedDeps              a map of all {@link DependencyNode}s already grouped to {@link DescriptorFileTreeNode}s. Newly grouped DependencyNodes will be added to this map.
     */
    private void visitDepTreeNode(Map<String, DependencyNode> vulnerableDependencies, DepTree depTree, List<String> path,
                                  Map<String, DescriptorFileTreeNode> descriptorNodes, List<String> descriptorPaths,
                                  Map<String, Map<String, DependencyNode>> addedDeps) {
        String compId = path.get(path.size() - 1);
        DepTreeNode compNode = depTree.getNodes().get(compId);
        List<String> innerDescriptorPaths = descriptorPaths;
        if (compNode.getDescriptorFilePath() != null) {
            innerDescriptorPaths = new ArrayList<>(descriptorPaths);
            innerDescriptorPaths.add(compNode.getDescriptorFilePath());
        }
        if (vulnerableDependencies.containsKey(compId)) {
            DependencyNode dependencyNode = vulnerableDependencies.get(compId);
            addImpactPathToDependencyNode(dependencyNode, path);

            DepTreeNode parentCompNode = null;
            if (path.size() >= 2) {
                String parentCompId = path.get(path.size() - 2);
                parentCompNode = depTree.getNodes().get(parentCompId);
            }
            for (String descriptorPath : innerDescriptorPaths) {
                boolean indirect = parentCompNode != null && !descriptorPath.equals(parentCompNode.getDescriptorFilePath());
                if (!descriptorNodes.containsKey(descriptorPath)) {
                    descriptorNodes.put(descriptorPath, new DescriptorFileTreeNode(descriptorPath));
                    addedDeps.put(descriptorPath, new HashMap<>());
                }
                DependencyNode existingDep = addedDeps.get(descriptorPath).get(compId);
                if (existingDep != null) {
                    // If this dependency has any direct path, then it's direct
                    if (existingDep.isIndirect() && !indirect) {
                        existingDep.setIndirect(false);
                    }
                    continue;
                }
                // Each dependency might be a child of more than one descriptor, but DependencyNode is a tree node, so it can have only one parent.
                // The solution for this is to clone the dependency before adding it as a child of the POM.
                DependencyNode clonedDep = (DependencyNode) dependencyNode.clone();
                clonedDep.setIndirect(indirect);

                descriptorNodes.get(descriptorPath).addDependency(clonedDep);
                addedDeps.get(descriptorPath).put(compId, clonedDep);
            }
        }

        for (String childId : compNode.getChildren()) {
            List<String> pathToChild = new ArrayList<>(path);
            pathToChild.add(childId);
            if (!path.contains(childId)) {
                visitDepTreeNode(vulnerableDependencies, depTree, pathToChild, descriptorNodes, innerDescriptorPaths, addedDeps);
            }
        }
    }

    private void addImpactPathToDependencyNode(DependencyNode dependencyNode, List<String> path) {
        if (dependencyNode.getImpactTree() == null) {
            dependencyNode.setImpactTree(new ImpactTree(new ImpactTreeNode(path.get(0))));
        }
        ImpactTree impactTree = dependencyNode.getImpactTree();
        impactTree.incImpactPathsCount();
        if (impactTree.getImpactPathsCount() > IMPACT_PATHS_LIMIT) {
            return;
        }
        ImpactTreeNode parentImpactTreeNode = impactTree.getRoot();
        for (int pathNodeIndex = 1; pathNodeIndex < path.size(); pathNodeIndex++) {
            String currPathNode = path.get(pathNodeIndex);
            // Find a child of parentImpactTreeNode with a name equals to currPathNode
            ImpactTreeNode currImpactTreeNode = parentImpactTreeNode.getChildren().stream().filter(impactTreeNode -> impactTreeNode.getName().equals(currPathNode)).findFirst().orElse(null);
            if (currImpactTreeNode == null) {
                currImpactTreeNode = new ImpactTreeNode(currPathNode);
                parentImpactTreeNode.getChildren().add(currImpactTreeNode);
            }
            parentImpactTreeNode = currImpactTreeNode;
        }
    }

    /**
     * Launch async dependency scan.
     */
    void asyncScanAndUpdateResults() {
        if (DumbService.isDumb(project)) { // If intellij is still indexing the project
            return;
        }
        // The tasks run asynchronously. To make sure no more than 3 tasks are running concurrently,
        // we use a count-down latch that signals to that executor service that it can get more tasks.
        CountDownLatch latch = new CountDownLatch(1);
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(null, getTaskTitle()) {
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
                    log.info("Scan already in progress");
                    return;
                }
                progressIndicator = indicator;
                scanAndUpdate(new ProgressIndicatorImpl(indicator));
            }

            @Override
            public void onFinished() {
                latch.countDown();
                scanInProgress.set(false);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                log.error(ExceptionUtils.getRootCauseMessage(error));
                scanError.set(true);
            }

        };
        executor.submit(createRunnable(scanAndUpdateTask, latch, progressIndicator, log));
    }

    /**
     * Stop the current scan.
     */
    void stopScan() {
        if (progressIndicator != null) {
            progressIndicator.cancel();
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
        return "JFrog scanning " + StringUtils.defaultIfBlank(relativePath, project.getName());
    }

    /**
     * Create a runnable to be submitted to the executor service, or run directly.
     *
     * @param task              The task to submit
     * @param latch             The countdown latch, which makes sure the executor service doesn't get more than 3 tasks.
     *                          If null, the scan was initiated by a change in the project descriptor and the executor
     *                          service is terminated. In this case, there is no requirement to wait.
     * @param progressIndicator The task's {@link com.intellij.openapi.progress.ProgressIndicator} object.
     */
    public static Runnable createRunnable(Task.Backgroundable task, CountDownLatch latch, com.intellij.openapi.progress.ProgressIndicator progressIndicator, Log log) {
        return () -> {
            // The progress manager is only good for foreground threads.
            if (SwingUtilities.isEventDispatchThread()) {
                task.queue();
            } else {
                // Run the scan task when the thread is in the foreground.
                ApplicationManager.getApplication().invokeLater(task::queue);
            }
            try {
                // Wait for scan to finish, to make sure the thread pool remain full
                if (latch != null) {
                    latch.await();
                }
            } catch (InterruptedException e) {
                // This exception is thrown when this thread is interrupted (e.g. when the scan timeout has elapsed).
                logError(log, ExceptionUtils.getRootCauseMessage(e), e, false);
                progressIndicator.cancel();
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
        DumbService.getInstance(project).smartInvokeLater(() -> {
            PsiFile[] projectDescriptors = getProjectDescriptors();
            if (ArrayUtils.isEmpty(projectDescriptors)) {
                return;
            }
            GlobalInspectionContext context = InspectionManager.getInstance(project).createNewGlobalContext();
            AbstractInspection localInspectionTool = getInspectionTool();
            if (localInspectionTool == null) {
                return;
            }
            localInspectionTool.setAfterScan(true);
            for (PsiFile descriptor : projectDescriptors) {
                // Run inspection on descriptor.
                InspectionEngine.runInspectionOnFile(descriptor, new LocalInspectionToolWrapper(localInspectionTool), context);
                FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors(descriptor.getVirtualFile());
                if (!ArrayUtils.isEmpty(editors)) {
                    // Refresh descriptor highlighting only if it is already opened.
                    DaemonCodeAnalyzer.getInstance(project).restart(descriptor);
                }
            }
        });
    }

    private void addScanResults(List<FileTreeNode> fileTreeNodes) {
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

    boolean isScanErrorOccurred() {
        return this.scanError.get();
    }

    boolean isScanCanceled() {
        return this.scanCanceled.get();
    }

    public String getProjectPath() {
        return this.basePath;
    }
}
