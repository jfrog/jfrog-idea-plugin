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
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencies;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
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
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.utils.Utils;
import com.jfrog.xray.client.services.summary.Components;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by romang on 4/26/17.
 */
public abstract class ScanManager extends ScanManagerBase {

    private static final Path HOME_PATH = Paths.get(System.getProperty("user.home"), ".jfrog-idea-plugin");
    protected Project mainProject;
    Project project;

    // Lock to prevent multiple simultaneous scans
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    /**
     * @param mainProject - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                    like {@link FilterManagerService} and {@link ComponentsTree}.
     * @param project     - Current working project.
     * @param prefix      - Components prefix for xray scan, e.g. gav:// or npm://.
     */
    ScanManager(@NotNull Project mainProject, @NotNull Project project, ComponentPrefix prefix) throws IOException {
        super(HOME_PATH.resolve("cache"), project.getName(), Logger.getInstance(), GlobalSettings.getInstance().getXrayConfig(), prefix);
        this.mainProject = mainProject;
        this.project = project;
        Files.createDirectories(HOME_PATH);
        registerOnChangeHandlers();
    }

    /**
     * Refresh project dependencies.
     */
    protected abstract void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<ProjectDependencies>> dependenciesData);

    /**
     * Collect and return {@link Components} to be scanned by JFrog Xray.
     * Implementation should be project type specific.
     */
    protected abstract void buildTree(@Nullable DataNode<ProjectData> externalProject) throws IOException;

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

    /**
     * Scan and update dependency components.
     */
    private void scanAndUpdate(boolean quickScan, ProgressIndicator indicator, @Nullable Collection<DataNode<ProjectDependencies>> dependenciesData) {
        // Don't scan if Xray is not configured
        if (!GlobalSettings.getInstance().areCredentialsSet()) {
            getLog().error("Xray server is not configured.");
            return;
        }
        // Prevent multiple simultaneous scans
        if (!scanInProgress.compareAndSet(false, true)) {
            if (!quickScan) {
                getLog().info("Scan already in progress");
            }
            return;
        }
        try {
            // Refresh dependencies -> Collect -> Scan and store to cache -> Update view
            refreshDependencies(getRefreshDependenciesCbk(quickScan, indicator), dependenciesData);
        } finally {
            scanInProgress.set(false);
        }
    }

    /**
     * Launch async dependency scan.
     */
    void asyncScanAndUpdateResults(boolean quickScan, @Nullable Collection<DataNode<ProjectDependencies>> dependenciesData) {
        if (DumbService.isDumb(mainProject)) { // If intellij is still indexing the project
            return;
        }
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(null, "Xray: Scanning for vulnerabilities...") {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                if (project.isDisposed()) {
                    return;
                }
                scanAndUpdate(quickScan, new ProgressIndicatorImpl(indicator), dependenciesData);
            }
        };
        // The progress manager is only good for foreground threads.
        if (SwingUtilities.isEventDispatchThread()) {
            ProgressManager.getInstance().run(scanAndUpdateTask);
        } else {
            // Run the scan task when the thread is in the foreground.
            ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(scanAndUpdateTask));
        }
    }

    /**
     * Returns all project modules locations as Paths.
     * Other scanners such as npm will use this paths in order to find modules.
     *
     * @return all project modules locations as Paths
     */
    public Set<Path> getProjectPaths() {
        Set<Path> paths = Sets.newHashSet();
        paths.add(Utils.getProjectBasePath(project));
        return paths;
    }

    /**
     * Launch async dependency scan.
     */
    void asyncScanAndUpdateResults() {
        asyncScanAndUpdateResults(true, null);
    }

    private ExternalProjectRefreshCallback getRefreshDependenciesCbk(boolean quickScan, ProgressIndicator indicator) {
        return new ExternalProjectRefreshCallback() {
            @Override
            public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
                try {
                    buildTree(externalProject);
                    scanAndCacheArtifacts(indicator, quickScan);
                    addXrayInfoToTree(getScanResults());
                    setScanResults();
                    DumbService.getInstance(mainProject).smartInvokeLater(() -> runInspections());
                } catch (ProcessCanceledException e) {
                    getLog().info("Xray scan was canceled");
                } catch (Exception e) {
                    getLog().error("", e);
                }
            }

            @Override
            public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
                getLog().error(StringUtils.defaultIfEmpty(errorDetails, errorMessage));
            }
        };
    }

    void runInspections() {
        PsiFile[] projectDescriptors = getProjectDescriptors();
        if (ArrayUtils.isEmpty(projectDescriptors)) {
            return;
        }
        InspectionManagerEx inspectionManagerEx = (InspectionManagerEx) InspectionManager.getInstance(mainProject);
        GlobalInspectionContext context = inspectionManagerEx.createNewGlobalContext(false);
        LocalInspectionTool localInspectionTool = getInspectionTool();
        for (PsiFile descriptor : projectDescriptors) {
            // Run inspection on descriptor.
            InspectionEngine.runInspectionOnFile(descriptor, new LocalInspectionToolWrapper(localInspectionTool), context);
            FileEditor[] editors = FileEditorManager.getInstance(mainProject).getAllEditors(descriptor.getVirtualFile());
            if (!ArrayUtils.isEmpty(editors)) {
                // Refresh descriptor highlighting only if it is already opened.
                DaemonCodeAnalyzer.getInstance(mainProject).restart(descriptor);
            }
        }
    }

    private void registerOnChangeHandlers() {
        MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
        busConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, this::asyncScanAndUpdateResults);
    }

    /**
     * @return all licenses available from the current scan results.
     */
    public Set<License> getAllLicenses() {
        Set<License> allLicenses = Sets.newHashSet();
        if (getScanResults() == null) {
            return allLicenses;
        }
        DependenciesTree node = (DependenciesTree) getScanResults().getRoot();
        collectAllLicenses(node, allLicenses);
        return allLicenses;
    }

    /**
     * @return all scopes available from the current scan results.
     */
    public Set<Scope> getAllScopes() {
        Set<Scope> allScopes = Sets.newHashSet();
        if (getScanResults() == null) {
            return allScopes;
        }
        DependenciesTree node = (DependenciesTree) getScanResults().getRoot();
        collectAllScopes(node, allScopes);
        return allScopes;
    }

    /**
     * filter scan components tree model according to the user filters and sort the issues tree.
     */
    private void setScanResults() {
        DependenciesTree scanResults = getScanResults();
        if (scanResults == null) {
            return;
        }
        if (!scanResults.isLeaf()) {
            addLicensesAndScopes(FilterManagerService.getInstance(mainProject));
        }
        ProjectsMap.ProjectKey projectKey = ProjectsMap.createKey(getProjectName(),
                scanResults.getGeneralInfo());
        MessageBus projectMessageBus = mainProject.getMessageBus();

        ComponentsTree componentsTree = ComponentsTree.getInstance(mainProject);
        componentsTree.addScanResults(getProjectName(), scanResults);
        projectMessageBus.syncPublisher(ProjectEvents.ON_SCAN_PROJECT_CHANGE).update(projectKey);
    }

    @Override
    protected void checkCanceled() {
        if (project.isOpen()) {
            // The project is closed if we are in test mode.
            // In tests we can't check if the user canceled the scan, since we don't have the ProgressManager service.
            ProgressManager.checkCanceled();
        }
    }

    boolean isScanInProgress() {
        return this.scanInProgress.get();
    }

    public String getProjectPath() {
        return project.getBasePath();
    }

    /**
     * Subscribe ScanManager for VFS-change events.
     * Perform dependencies scan and update tree after the provided file has changed.
     *
     * @param fileName - file to track for changes.
     */
    protected void subscribeLaunchDependencyScanOnFileChangedEvents(String fileName) {
        String fileToSubscribe = Paths.get(Utils.getProjectBasePath(project).toString(), fileName).toString();
        mainProject.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
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
}
