package org.jfrog.idea.xray.scan;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.services.summary.ComponentDetail;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.Events;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.configuration.XrayServerConfig;
import org.jfrog.idea.xray.FilterManager;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.persistency.ScanCache;
import org.jfrog.idea.xray.persistency.types.Artifact;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.utils.Utils;

import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jfrog.idea.xray.utils.Utils.MINIMAL_XRAY_VERSION_SUPPORTED;

/**
 * Created by romang on 4/26/17.
 */
public abstract class ScanManager {

    protected final Project project;
    private TreeModel scanResults;
    private final static int NUMBER_OF_ARTIFACTS_BULK_SCAN = 100;

    // Lock to prevent multiple simultaneous scans
    AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private static final Logger log = Logger.getInstance(ScanManager.class);

    protected ScanManager(Project project) {
        this.project = project;
        registerOnChangeHandlers();
    }

    /**
     * Collect and return {@link Components} to be scanned by JFrog Xray.
     * Implementation should be project type specific.
     *
     * @return Components
     */
    protected abstract Components collectComponentsToScan();

    /**
     * Create new {@link TreeModel} scan results, old scan results available if needed.
     *
     * @param currentScanResults
     * @return TreeModel
     */
    protected abstract TreeModel updateResultsTree(TreeModel currentScanResults);

    /**
     * Scan and update dependency components.
     *
     * @param quickScan
     * @param indicator
     */
    private void scanAndUpdate(boolean quickScan, ProgressIndicator indicator) {
        // Don't scan if Xray is not configured
        if (!GlobalSettings.getInstance().isCredentialsSet()) {
            Notifications.Bus.notify(new Notification("JFrog", "JFrog Xray scan failed", "Xray server is not configured.", NotificationType.ERROR));
            return;
        }
        // Prevent multiple simultaneous scans
        if (!scanInProgress.compareAndSet(false, true)) {
            if (!quickScan) {
                Notifications.Bus.notify(new Notification("JFrog", "JFrog Xray", "Scan already in progress.", NotificationType.INFORMATION));
            }
            return;
        }

        try {
            // Collect -> Scan and store to cache -> update view
            Components components = collectComponentsToScan();
            scanAndCacheArtifacs(components, quickScan, indicator);
            scanResults = updateResultsTree(scanResults);
            MessageBus messageBus = project.getMessageBus();
            messageBus.syncPublisher(Events.ON_SCAN_COMPONENTS_CHANGE).update();
        } finally {
            scanInProgress.set(false);
        }
    }

    /**
     * Launch async dependency scan.
     *
     * @param quickScan
     */
    public void asyncScanAndUpdateResults(boolean quickScan) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Xray: scanning for vulnerabilities...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                scanAndUpdate(quickScan, indicator);
                indicator.finishNonCancelableSection();
            }
        });
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

    /**
     * @return all licenses available from the current scan results.
     */
    public Set<License> getAllLicenses() {
        Set<License> allLicenses = new HashSet<>();
        if (scanResults == null) {
            return allLicenses;
        }
        ScanTreeNode node = (ScanTreeNode) scanResults.getRoot();
        for (int i = 0; i < node.getChildCount(); i++) {
            allLicenses.addAll(((ScanTreeNode) node.getChildAt(i)).getLicenses());
        }
        return allLicenses;
    }

    /**
     * @return filtered scan components tree model according the user filters.
     */
    public TreeModel getFilteredScanTreeModel() {
        return FilterManager.getInstance(project).filterComponents(scanResults);
    }

    /**
     * @param node
     * @return filtered issues according to the selected component and user filters.
     */
    public TableModel getFilteredScanIssues(ScanTreeNode node) {
        return FilterManager.getInstance(project).filterIssues(node.getAllIssues());
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
    private void scanAndCacheArtifacs(Components components, boolean quickScan, ProgressIndicator indicator) {
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
            List<ComponentDetail> componentsList = componentsToScan.getComponentDetails();
            while (currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN < componentsList.size()) {
                if (indicator.isCanceled()) {
                    log.info("Xray scan was canceled");
                    return;
                }

                List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, currentIndex + NUMBER_OF_ARTIFACTS_BULK_SCAN);
                Components partialComponents = ComponentsFactory.create(partialComponentsDetails);
                scanComponents(xray, partialComponents);
                indicator.setFraction(((double) currentIndex + 1) / (double) componentsList.size());
                currentIndex += NUMBER_OF_ARTIFACTS_BULK_SCAN;
            }

            List<ComponentDetail> partialComponentsDetails = componentsList.subList(currentIndex, componentsList.size());
            Components partialComponents = ComponentsFactory.create(partialComponentsDetails);
            scanComponents(xray, partialComponents);
            indicator.setFraction(1);
        } catch (IOException e) {
            Notifications.Bus.notify(new Notification("JFrog", "JFrog Xray scan failed", e.getMessage(), NotificationType.ERROR));
        }
    }

    private boolean isXrayVersionSupported(Xray xray) {
        try {
            if (Utils.isXrayVersionSupported(xray.system().version())) {
                return true;
            }
            Notifications.Bus.notify(new Notification("JFrog", "Unsupported JFrog Xray version", "Required JFrog Xray version " + MINIMAL_XRAY_VERSION_SUPPORTED + " and above", NotificationType.ERROR));
        } catch (IOException e) {
            Notifications.Bus.notify(new Notification("JFrog", "JFrog Xray scan failed", e.getMessage(), NotificationType.ERROR));
        }
        return false;
    }

    private void scanComponents(Xray xray, Components artifactsToScan) throws IOException {
        ScanCache scanCache = ScanCache.getInstance(project);
        SummaryResponse summary = xray.summary().componentSummary(artifactsToScan);
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
}
