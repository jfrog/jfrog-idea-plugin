package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.services.system.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;

public class ScanManager {
    private final long SCAN_TIMEOUT_MINUTES = 10;
    private final Project project;
    private final ScannerFactory factory;
    private Map<Integer, ScannerBase> scanners = Maps.newHashMap();

    private ScanManager(@NotNull Project project) {
        this.project = project;
        factory = new ScannerFactory(project);
    }

    public static ScanManager getInstance(@NotNull Project project) {
        return project.getService(ScanManager.class);
    }

    public static Set<ScannerBase> getScanners(@NotNull Project project) {
        ScanManager scanManager = getInstance(project);
        return Sets.newHashSet(scanManager.scanners.values());
    }

    /**
     * Start an Xray scan for all projects.
     */
    public void startScan() {
        if (DumbService.isDumb(project)) { // If intellij is still indexing the project
            return;
        }

        if (isScanInProgress()) {
            Logger.getInstance().info("Previous scan still running...");
            return;
        }

        if (!GlobalSettings.getInstance().areXrayCredentialsSet()) {
            Logger.getInstance().warn("Xray server is not configured.");
            return;
        }

        project.getMessageBus().syncPublisher(ApplicationEvents.ON_SCAN_LOCAL_STARTED).update();
        LocalComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        componentsTree.setScanningEmptyText();
        Thread currScanThread = new Thread(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(3);
            try {
                ScanLogic scanLogic = createScanLogic();
                refreshScanners(scanLogic, executor);
                NavigationService.clearNavigationMap(project);
                for (ScannerBase scanner : scanners.values()) {
                    try {
                        scanner.asyncScanAndUpdateResults();
                    } catch (RuntimeException e) {
                        logError(Logger.getInstance(), "", e, true);
                    }
                }
                executor.shutdown();
                //noinspection ResultOfMethodCallIgnored
                executor.awaitTermination(SCAN_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                componentsTree.cacheTree();
            } catch (IOException | RuntimeException | InterruptedException e) {
                logError(Logger.getInstance(), "", e, true);
            } finally {
                executor.shutdownNow();
                componentsTree.setNoIssuesEmptyText();
            }
        });
        currScanThread.start();
    }

    /**
     * Scan projects, create new Scanners and delete unnecessary ones.
     */
    public void refreshScanners(ScanLogic scanLogic, @Nullable ExecutorService executor) throws IOException, InterruptedException {
        scanners = factory.refreshScanners(scanners, scanLogic, executor);
    }

    public boolean isScanInProgress() {
        return scanners.values().stream().anyMatch(ScannerBase::isScanInProgress);
    }

    /**
     * Create the scan logic according to Xray version.
     *
     * @return Xray scan logic
     */
    private ScanLogic createScanLogic() throws IOException {
        Logger logger = Logger.getInstance();
        ServerConfig server = GlobalSettings.getInstance().getServerConfig();
        try (XrayClient client = createXrayClientBuilder(server, Logger.getInstance()).build()) {
            Version xrayVersion = client.system().version();
            if (GraphScanLogic.isSupportedInXrayVersion(xrayVersion)) {
                return new GraphScanLogic(logger);
            }
            throw new IOException("Unsupported JFrog Xray version.");
        }
    }
}
