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
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanManagerBase;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.ui.menus.filtermanager.LocalFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import com.jfrog.xray.client.services.summary.Components;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
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
    protected abstract void buildTree(boolean shouldToast) throws IOException;

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
    protected abstract LocalInspectionTool getInspectionTool();

    protected void sendUsageReport() {
        Utils.sendUsageReport(getProjectPackageType() + "-deps");
    }

    protected abstract String getProjectPackageType();

    /**
     * Scan and update dependency components.
     *
     * @param quickScan - Quick scan or full scan
     * @param indicator - The progress indicator
     */
    private void scanAndUpdate(boolean quickScan, ProgressIndicator indicator) {
        try {
            indicator.setText("1/3: Building dependency tree");
            buildTree(!quickScan);
            indicator.setText("2/3: Xray scanning project dependencies");
            scanAndCacheArtifacts(indicator, quickScan);
            indicator.setText("3/3: Finalizing");
            addXrayInfoToTree(getScanResults());
            setScanResults();
            DumbService.getInstance(project).smartInvokeLater(this::runInspections);
        } catch (ProcessCanceledException e) {
            getLog().info("Xray scan was canceled");
        } catch (Exception e) {
            logError(getLog(), "Xray Scan failed", e, !quickScan);
        } finally {
            scanInProgress.set(false);
            sendUsageReport();
        }
    }

    /**
     * Launch async dependency scan.
     */
    void asyncScanAndUpdateResults(boolean quickScan) {
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
                    if (!quickScan) {
                        getLog().info("Scan already in progress");
                    }
                    return;
                }
                scanAndUpdate(quickScan, new ProgressIndicatorImpl(indicator));
            }

            @Override
            public void onFinished() {
                latch.countDown();
            }
        };
        if (executor.isShutdown() || executor.isTerminated()) {
            // Scan initiated by a change in the project descriptor
            createRunnable(scanAndUpdateTask, null, quickScan).run();
        } else {
            // Scan initiated by opening IntelliJ, by user, or by changing the configuration
            executor.submit(createRunnable(scanAndUpdateTask, latch, quickScan));
        }
    }

    /**
     * Get text to display in the task progress.
     *
     * @return text to display in the task progress.
     */
    private String getTaskTitle() {
        String relativePath = Utils.getProjectBasePath(project).relativize(Paths.get(basePath)).toString();
        return "Xray scanning " + StringUtils.defaultIfBlank(relativePath, project.getName());
    }

    /**
     * Create a runnable to be submitted to the executor service, or run directly.
     *
     * @param scanAndUpdateTask - The task to submit
     * @param latch             - The countdown latch, which makes sure the executor service doesn't get more than 3 tasks.
     *                          If null, the scan was initiated by a change in the project descriptor and the executor
     *                          service is terminated. In this case, there is no requirement to wait.
     * @param quickScan         - Quick or full scan
     */
    private Runnable createRunnable(Task.Backgroundable scanAndUpdateTask, CountDownLatch latch, boolean quickScan) {
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
                logError(getLog(), ExceptionUtils.getRootCauseMessage(e), e, !quickScan);
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

    /**
     * Launch async dependency scan.
     */
    void asyncScanAndUpdateResults() {
        asyncScanAndUpdateResults(true);
    }

    void runInspections() {
        PsiFile[] projectDescriptors = getProjectDescriptors();
        if (ArrayUtils.isEmpty(projectDescriptors)) {
            return;
        }
        InspectionManagerEx inspectionManagerEx = (InspectionManagerEx) InspectionManager.getInstance(project);
        GlobalInspectionContext context = inspectionManagerEx.createNewGlobalContext(false);
        LocalInspectionTool localInspectionTool = getInspectionTool();
        for (PsiFile descriptor : projectDescriptors) {
            // Run inspection on descriptor.
            InspectionEngine.runInspectionOnFile(descriptor, new LocalInspectionToolWrapper(localInspectionTool), context);
            FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors(descriptor.getVirtualFile());
            if (!ArrayUtils.isEmpty(editors)) {
                // Refresh descriptor highlighting only if it is already opened.
                DaemonCodeAnalyzer.getInstance(project).restart(descriptor);
            }
        }
    }

    /**
     * @return all licenses available from the current scan results.
     */
    public Set<License> getAllLicenses() {
        if (getScanResults() == null) {
            return Sets.newHashSet();
        }
        return collectAllLicenses((DependencyTree) getScanResults().getRoot());
    }

    /**
     * @return all scopes available from the current scan results.
     */
    public Set<Scope> getAllScopes() {
        if (getScanResults() == null) {
            return Sets.newHashSet();
        }
        return collectAllScopes((DependencyTree) getScanResults().getRoot());
    }

    /**
     * Return true if the scan results contain any issues.
     *
     * @return true if the scan results contain any issues.
     */
    public boolean isContainIssues() {
        return getScanResults() != null && !getScanResults().getIssues().isEmpty();
    }

    /**
     * Return true if the scan results contain any violated licenses.
     *
     * @return true if the scan results contain any violated licenses.
     */
    public boolean isContainViolatedLicenses() {
        return getScanResults() != null && !getScanResults().getViolatedLicenses().isEmpty();
    }

    /**
     * filter scan components tree model according to the user filters and sort the issues tree.
     */
    private void setScanResults() {
        DependencyTree scanResults = getScanResults();
        if (scanResults == null) {
            return;
        }
        if (!scanResults.isLeaf()) {
            LocalFilterManager.getInstance(project).collectsFiltersInformation(scanResults);
        }
        ProjectsMap.ProjectKey projectKey = ProjectsMap.createKey(getProjectName(),
                scanResults.getGeneralInfo());
        MessageBus projectMessageBus = project.getMessageBus();

        ComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        componentsTree.addScanResults(getProjectName(), scanResults);
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

    /**
     * Subscribe ScanManager for VFS-change events.
     * Perform dependencies scan and update tree after the provided file has changed.
     *
     * @param fileName - file to track for changes.
     */
    protected void subscribeLaunchDependencyScanOnFileChangedEvents(String fileName) {
        String fileToSubscribe = Paths.get(basePath, fileName).toString();
        busConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    String filePath = event.getPath();
                    if (StringUtils.equals(filePath, fileToSubscribe)) {
                        asyncScanAndUpdateResults();
                    }
                }
            }
        });
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the bus connection
        busConnection.disconnect();
    }
}
