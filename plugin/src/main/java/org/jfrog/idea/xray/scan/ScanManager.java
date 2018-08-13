package org.jfrog.idea.xray.scan;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.impl.services.summary.ComponentDetailImpl;
import com.jfrog.xray.client.services.summary.ComponentDetail;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.idea.Events;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.configuration.XrayServerConfig;
import org.jfrog.idea.ui.xray.models.IssuesTableModel;
import org.jfrog.idea.xray.FilterManager;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.persistency.ScanCache;
import org.jfrog.idea.xray.persistency.types.Artifact;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.utils.Utils;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jfrog.idea.xray.utils.Utils.MINIMAL_XRAY_VERSION_SUPPORTED;

/**
 * Created by romang on 4/26/17.
 */
public abstract class ScanManager {

    static final String ROOT_NODE_HEADER = "All components";
    boolean isMultimoduleProject;
    static final String GAV_PREFIX = "gav://";
    private final static int NUMBER_OF_ARTIFACTS_BULK_SCAN = 100;

    Project project;
    private TreeModel scanResults;
    static final Logger logger = Logger.getInstance(ScanManager.class);

    // Lock to prevent multiple simultaneous scans
    private AtomicBoolean scanInProgress = new AtomicBoolean(false);

    ScanManager() {
    }

    ScanManager(Project project, boolean isMultiModule) {
        this.project = project;
        this.isMultimoduleProject = isMultiModule;
        registerOnChangeHandlers();
    }

    /**
     * Refresh project dependencies.
     */
    protected abstract void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies);

    /**
     * Collect and return {@link Components} to be scanned by JFrog Xray.
     * Implementation should be project type specific.
     */
    protected abstract Components collectComponentsToScan(@Nullable DataNode<ProjectData> externalProject);

    /**
     * Create new {@link TreeModel} scan results, old scan results available if needed.
     */
    protected abstract TreeModel updateResultsTree(TreeModel currentScanResults);

    /**
     * Populate a ScanTreeNode with issues, licenses and general info from the scan cache.
     */
    void populateScanTreeNode(ScanTreeNode scanTreeNode) {
        Artifact scanArtifact = getArtifactSummary(scanTreeNode.toString());
        if (scanArtifact != null) {
            scanTreeNode.setIssues(Sets.newHashSet(scanArtifact.issues));
            scanTreeNode.setLicenses(Sets.newHashSet(scanArtifact.licenses));
            scanTreeNode.setGeneralInfo(scanArtifact.general);
        }
    }

    void scanTree(ScanTreeNode rootNode) {
        rootNode.getChildren().forEach(child -> {
            populateScanTreeNode(child);
            scanTree(child);
        });
    }

    void addAllArtifacts(Components components, ScanTreeNode rootNode, String prefix) {
        rootNode.getChildren().forEach(child -> {
            if (!child.isModule()) {
                ComponentDetailImpl scanComponent = (ComponentDetailImpl) child.getUserObject();
                components.addComponent(prefix + scanComponent.getComponentId(), scanComponent.getSha1());
            }
            addAllArtifacts(components, child, prefix);
        });
    }

    /**
     * Scan and update dependency components.
     */
    private void scanAndUpdate(boolean quickScan, ProgressIndicator indicator, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        // Don't scan if Xray is not configured
        if (!GlobalSettings.getInstance().isCredentialsSet()) {
            Utils.notify(logger, "JFrog Xray scan failed", "Xray server is not configured.", NotificationType.ERROR);
            return;
        }
        // Prevent multiple simultaneous scans
        if (!scanInProgress.compareAndSet(false, true)) {
            if (!quickScan) {
                Utils.notify(logger, "JFrog Xray", "Scan already in progress.", NotificationType.INFORMATION);
            }
            return;
        }
        try {
            // Refresh dependencies -> Collect -> Scan and store to cache -> Update view
            refreshDependencies(getRefreshDependenciesCbk(quickScan, indicator), libraryDependencies);
        } finally {
            scanInProgress.set(false);
        }
    }

    /**
     * Launch async dependency scan.
     */
    public void asyncScanAndUpdateResults(boolean quickScan, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(project, "Xray: Scanning for Vulnerabilities...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                scanAndUpdate(quickScan, indicator, libraryDependencies);
                indicator.finishNonCancelableSection();
            }
        };
        // The progress manager is only good for foreground threads.
        if (SwingUtilities.isEventDispatchThread()) {
            ProgressManager.getInstance().run(scanAndUpdateTask);
        } else {
            // Run the scan task when the thread is in the foreground.
            SwingUtilities.invokeLater(() -> ProgressManager.getInstance().run(scanAndUpdateTask));
        }
    }

    /**
     * Launch async dependency scan.
     */
    public void asyncScanAndUpdateResults(boolean quickScan) {
        asyncScanAndUpdateResults(quickScan, null);
    }

    private ExternalProjectRefreshCallback getRefreshDependenciesCbk(boolean quickScan, ProgressIndicator indicator) {
        return new ExternalProjectRefreshCallback() {
            @Override
            public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
                try {
                    Components components = collectComponentsToScan(externalProject);
                    scanAndCacheArtifacts(components, quickScan, indicator);
                    scanResults = updateResultsTree(scanResults);
                    setUiLicenses();
                    MessageBus messageBus = project.getMessageBus();
                    messageBus.syncPublisher(Events.ON_SCAN_COMPONENTS_CHANGE).update();
                } catch (Exception e) {
                    Utils.notify(logger, "JFrog Xray scan failed", e, NotificationType.ERROR);
                }
            }

            @Override
            public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
                String details;
                String title = "JFrog Xray scan failed";
                if (errorDetails != null) {
                    details = errorDetails;
                    title += ": " + errorMessage;
                } else {
                    details = errorMessage;
                }
                Utils.notify(logger, title, details, NotificationType.ERROR);
            }
        };
    }

    private void registerOnChangeHandlers() {
        MessageBusConnection busConnection = project.getMessageBus().connect(project);
        busConnection.subscribe(Events.ON_SCAN_FILTER_CHANGE, () -> {
            MessageBus messageBus = project.getMessageBus();
            messageBus.syncPublisher(Events.ON_SCAN_COMPONENTS_CHANGE).update();
            messageBus.syncPublisher(Events.ON_SCAN_ISSUES_CHANGE).update();
        });

        busConnection.subscribe(Events.ON_CONFIGURATION_DETAILS_CHANGE,
                () -> asyncScanAndUpdateResults(true));
    }

    private void setUiLicenses() {
        FilterManager.getInstance(project).setLicenses(getAllLicenses());
    }

    /**
     * @return all licenses available from the current scan results.
     */
    public Set<License> getAllLicenses() {
        Set<License> allLicenses = new HashSet<>();
        if (scanResults == null) {
            return allLicenses;
        }
        ScanTreeNode node = (ScanTreeNode) scanResults.getRoot();
        getAllLicenses(node, allLicenses);
        return allLicenses;
    }

    private void getAllLicenses(ScanTreeNode node, Set<License> allLicenses) {
        allLicenses.addAll(node.getLicenses());
        node.getChildren().forEach(child -> getAllLicenses(child, allLicenses));
    }

    /**
     * filter scan components tree model according to the user filters and sort the issues tree.
     */
    public void filterAndSort(TreeModel issuesTreeModel, TreeModel licensesTreeModel) {
        if (scanResults == null) {
            return;
        }
        FilterManager filterManager = FilterManager.getInstance(project);
        ScanTreeNode issuesFilteredRoot = (ScanTreeNode) issuesTreeModel.getRoot();
        ScanTreeNode licenseFilteredRoot = (ScanTreeNode) licensesTreeModel.getRoot();
        filterManager.applyFilters((ScanTreeNode) scanResults.getRoot(), issuesFilteredRoot, licenseFilteredRoot);
        issuesFilteredRoot.setIssues(issuesFilteredRoot.processTreeIssues());
    }

    /**
     * return filtered issues according to the selected component and user filters.
     */
    public TableModel getFilteredScanIssues(List<ScanTreeNode> selectedNodes) {
        FilterManager filterManager = FilterManager.getInstance(project);
        Set<Issue> filteredIssues = Sets.newHashSet();
        selectedNodes.forEach(node -> filteredIssues.addAll(filterManager.filterIssues(node.getIssues())));
        return new IssuesTableModel(filteredIssues);
    }

    /**
     * @param componentId artifact component ID
     * @return {@link Artifact} according to the component ID.
     */
    Artifact getArtifactSummary(String componentId) {
        ScanCache scanCache = ScanCache.getInstance(project);
        return scanCache.getArtifact(componentId);
    }

    /**
     * Scan and cache components.
     *
     * @param components {@link Components} to be scanned.
     * @param quickScan  quick or full scan.
     * @param indicator  UI indicator.
     */
    private void scanAndCacheArtifacts(Components components, boolean quickScan, ProgressIndicator indicator) {
        if (components == null) {
            return;
        }

        ScanCache scanCache = ScanCache.getInstance(project);
        Components componentsToScan = ComponentsFactory.create();
        for (ComponentDetail details : components.getComponentDetails()) {
            String component = Utils.removeComponentIdPrefix(details.getComponentId());
            LocalDateTime dateTime = scanCache.getLastUpdateTime(component);
            if (!quickScan || dateTime == null || LocalDateTime.now().minusWeeks(1).isAfter(dateTime)) {
                componentsToScan.addComponent(details.getComponentId(), details.getSha1());
            }
        }

        if (componentsToScan.getComponentDetails().isEmpty()) {
            return;
        }

        XrayServerConfig xrayConfig = GlobalSettings.getInstance().getXrayConfig();
        Xray xray = XrayClient.create(xrayConfig.getUrl(), xrayConfig.getUsername(), xrayConfig.getPassword());

        if (!isXrayVersionSupported(xray)) {
            return;
        }

        try {
            int currentIndex = 0;
            List<ComponentDetail> componentsList = Lists.newArrayList(componentsToScan.getComponentDetails());
            while (currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN < componentsList.size()) {
                checkCanceled();
                List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN);
                Components partialComponents = ComponentsFactory.create(Sets.newHashSet(partialComponentsDetails));
                scanComponents(xray, partialComponents);
                indicator.setFraction(((double) currentIndex + 1) / (double) componentsList.size());
                currentIndex += NUMBER_OF_ARTIFACTS_BULK_SCAN;
            }

            List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, componentsList.size());
            Components partialComponents = ComponentsFactory.create(Sets.newHashSet(partialComponentsDetails));
            scanComponents(xray, partialComponents);
            indicator.setFraction(1);
        } catch (ProcessCanceledException e) {
            Utils.notify(logger, "JFrog Xray","Xray scan was canceled", NotificationType.INFORMATION);
        } catch (IOException e) {
            Utils.notify(logger, "JFrog Xray scan failed", e, NotificationType.ERROR);
        }
    }

    private boolean isXrayVersionSupported(Xray xray) {
        try {
            if (Utils.isXrayVersionSupported(xray.system().version())) {
                return true;
            }
            Utils.notify(logger, "Unsupported JFrog Xray version", "Required JFrog Xray version " + MINIMAL_XRAY_VERSION_SUPPORTED + " and above", NotificationType.ERROR);
        } catch (IOException e) {
            Utils.notify(logger, "JFrog Xray scan failed", e, NotificationType.ERROR);
        }
        return false;
    }

    private void scanComponents(Xray xray, Components artifactsToScan) throws IOException {
        ScanCache scanCache = ScanCache.getInstance(project);
        SummaryResponse summary = xray.summary().component(artifactsToScan);
        // Update cached artifact summary
        for (com.jfrog.xray.client.services.summary.Artifact summaryArtifact : summary.getArtifacts()) {
            if (summaryArtifact == null || summaryArtifact.getGeneral() == null) {
                continue;
            }
            String componentId = summaryArtifact.getGeneral().getComponentId();
            scanCache.updateArtifact(componentId, summaryArtifact);
            scanCache.setLastUpdated(componentId);
        }
    }

    static String getProjectBasePath(Project project) {
        return project.getBasePath() != null ? project.getBasePath() : "./";
    }

    void checkCanceled() {
        if (project.isOpen()) {
            // The project is closed if we are in test mode.
            // In tests we can't check if the user canceled the scan, since we don't have the ProgressManager service.
            ProgressManager.checkCanceled();
        }
    }
}
