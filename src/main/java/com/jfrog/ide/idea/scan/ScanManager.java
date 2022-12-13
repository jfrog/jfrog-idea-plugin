package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanManagerBase;
import com.jfrog.ide.common.tree.Artifact;
import com.jfrog.ide.common.tree.DescriptorFileTreeNode;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.events.ProjectEvents;
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
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;
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
public abstract class ScanManager extends ScanManagerBase implements Disposable {

    private final MessageBusConnection busConnection;
    private ExecutorService executor;
    // TODO: remove if used only in scanAndUpdate.
    private Collection<Artifact> depScanResults;
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
        super(basePath, Logger.getInstance(), GlobalSettings.getInstance().getServerConfig(), prefix);
        this.busConnection = project.getMessageBus().connect(this);
        this.executor = executor;
        this.basePath = basePath;
        this.project = project;
    }

    void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Collect and return {@link Components} to be scanned by JFrog Xray.
     * Implementation should be project type specific.
     *
     * @param shouldToast - True if should pop up a balloon when an error occurs.
     */
    protected abstract DependencyTree buildTree(boolean shouldToast) throws IOException;

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

    /**
     * Scan and update dependency components.
     *
     * @param shouldToast - True to enable showing balloons logs.
     * @param indicator   - The progress indicator
     */
    private void scanAndUpdate(boolean shouldToast, ProgressIndicator indicator) {
        try {
            indicator.setText("1/3: Building dependency tree");
            DependencyTree dependencyTree = buildTree(shouldToast);
            indicator.setText("2/3: Xray scanning project dependencies");
            Map<String, Artifact> results = scanArtifacts(indicator, dependencyTree);
            indicator.setText("3/3: Finalizing");
            // TODO: convert results to tree, and save it to cache!
            // TODO: set descriptor file path
            depScanResults = results.values();
            DescriptorFileTreeNode fileTreeNode = new DescriptorFileTreeNode("path/to/descriptor/file");
            fileTreeNode.addDependencies(depScanResults);
            // TODO: this method will also convert the tree to the new format:
//            BasicTreeNode descriptorNode = createDescriptorNode();
            setScanResults(fileTreeNode, dependencyTree.getGeneralInfo().getPath());

            // TODO: uncomment
//            DumbService.getInstance(project).smartInvokeLater(this::runInspections);
        } catch (ProcessCanceledException e) {
            getLog().info("Xray scan was canceled");
        } catch (Exception e) {
            logError(getLog(), "Xray Scan failed", e, shouldToast);
        } finally {
            scanInProgress.set(false);
            sendUsageReport();
        }
    }

    /**
     * Launch async dependency scan.
     *
     * @param shouldToast - True to enable showing balloons logs.
     */
    void asyncScanAndUpdateResults(boolean shouldToast) {
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
                if (!GlobalSettings.getInstance().areXrayCredentialsSet()) {
                    getLog().warn("Xray server is not configured.");
                    return;
                }
                // Prevent multiple simultaneous scans
                if (!scanInProgress.compareAndSet(false, true)) {
                    if (shouldToast) {
                        getLog().info("Scan already in progress");
                    }
                    return;
                }
                scanAndUpdate(shouldToast, new ProgressIndicatorImpl(indicator));
            }

            @Override
            public void onFinished() {
                latch.countDown();
            }
        };
        if (executor.isShutdown() || executor.isTerminated()) {
            // Scan initiated by a change in the project descriptor
            createRunnable(scanAndUpdateTask, null, shouldToast).run();
        } else {
            // Scan initiated by opening IntelliJ, by user, or by changing the configuration
            executor.submit(createRunnable(scanAndUpdateTask, latch, shouldToast));
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
     * @param shouldToast       - True to enable showing balloons logs.
     */
    private Runnable createRunnable(Task.Backgroundable scanAndUpdateTask, CountDownLatch latch, boolean shouldToast) {
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
                logError(getLog(), ExceptionUtils.getRootCauseMessage(e), e, shouldToast);
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
    private void setScanResults(DescriptorFileTreeNode fileTreeNode, String projectPath) {
        // TODO: make sure that it's null, if there are no violations
        if (fileTreeNode == null) {
            return;
        }
        ProjectsMap.ProjectKey projectKey = ProjectsMap.createKey(getProjectName(), projectPath);
        MessageBus projectMessageBus = project.getMessageBus();

        LocalComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        componentsTree.addScanResults(getProjectName(), fileTreeNode);
        projectMessageBus.syncPublisher(ProjectEvents.ON_SCAN_PROJECT_CHANGE).update(projectKey);
    }

    @Override
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

    @Override
    public void dispose() {
        // Disconnect and release resources from the bus connection
        busConnection.disconnect();
    }
}
