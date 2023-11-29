package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.ide.BrowserUtil;
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
import com.jfrog.xray.client.impl.util.JFrogInactiveEnvironmentException;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
    private final Project project;
    private final ScannerFactory factory;
    private final SourceCodeScannerManager sourceCodeScannerManager;
    private Map<Integer, ScannerBase> scanners = Maps.newHashMap();
    private ExecutorService executor;

    private ScanManager(@NotNull Project project) {
        this.project = project;
        factory = new ScannerFactory(project);
        sourceCodeScannerManager = new SourceCodeScannerManager(project);
    }

    public static ScanManager getInstance(@NotNull Project project) {
        return project.getService(ScanManager.class);
    }

    public static Set<ScannerBase> getScanners(@NotNull Project project) {
        ScanManager scanManager = getInstance(project);
        return Sets.newHashSet(scanManager.scanners.values());
    }

    public void runInspections(Project project) {
        try {
            refreshScanners(null, null);
            getScanners(project).forEach(ScannerBase::runInspections);
        } catch (InterruptedException | IOException e) {
            logError(Logger.getInstance(), "", e, false);
        }
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

        if (!GlobalSettings.getInstance().reloadMissingConfiguration()) {
            Logger.getInstance().error("Xray server is not configured.");
            return;
        }

        project.getMessageBus().syncPublisher(ApplicationEvents.ON_SCAN_LOCAL_STARTED).update();
        LocalComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        componentsTree.setScanningEmptyText();
        Thread currScanThread = new Thread(() -> {
            executor = Executors.newFixedThreadPool(3);
            try {
                // Source code scanners
                sourceCodeScannerManager.asyncScanAndUpdateResults(executor, Logger.getInstance());
                // Dependencies scanners
                ScanLogic scanLogic = createScanLogic();
                refreshScanners(scanLogic, executor);
                NavigationService.clearNavigationMap(project);
                for (ScannerBase scanner : scanners.values()) {
                    scanner.asyncScanAndUpdateResults();
                }
                executor.shutdown();
                if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES)) {
                    logError(Logger.getInstance(), "Scan timeout elapsed. The scan is being canceled.", true);
                }
                // Cache tree only if no errors occurred during scan.
                if (scanners.values().stream().anyMatch(ScannerBase::isScanErrorOccurred)) {
                    componentsTree.deleteCachedTree();
                    componentsTree.setScanErrorEmptyText();
                } else if (scanners.values().stream().anyMatch(ScannerBase::isScanCanceled)) {
                    project.getMessageBus().syncPublisher(ApplicationEvents.ON_SCAN_LOCAL_CANCELED).update();
                } else {
                    componentsTree.cacheTree();
                    componentsTree.setNoIssuesEmptyText();
                }
            } catch (JFrogInactiveEnvironmentException e) {
                handleJfrogInactiveEnvironment(e.getRedirectUrl());
                componentsTree.setScanErrorEmptyText();
            } catch (IOException | RuntimeException | InterruptedException e) {
                logError(Logger.getInstance(), ExceptionUtils.getRootCauseMessage(e), e, true);
                componentsTree.setScanErrorEmptyText();
            } finally {
                executor.shutdownNow();
            }
        });
        currScanThread.start();
    }

    public void stopScan() {
        executor.shutdown();
        scanners.values().forEach(ScannerBase::stopScan);
        sourceCodeScannerManager.stopScan();
    }

    /**
     * Handle inactive JFrog platform (free-tier) by displaying a clear warning message and a reactivation link.
     *
     * @param reactivationUrl is an URL to reactivate the specific free-tier platform.
     */
    private void handleJfrogInactiveEnvironment(String reactivationUrl) {
        Logger.getInstance().warn("JFrog Platform is not active.");
        Logger.showActionableBalloon(project, "JFrog Platform is not active.\nYou can activate it <a href=\"here\">here. </a>", () -> BrowserUtil.browse(reactivationUrl));
    }

    /**
     * Scan projects, create new Scanners and delete unnecessary ones.
     */
    public void refreshScanners(ScanLogic scanLogic, @Nullable ExecutorService executor) throws IOException, InterruptedException {
        scanners = factory.refreshScanners(scanners, scanLogic, executor);
    }

    public boolean isScanInProgress() {
        return scanners.values().stream().anyMatch(ScannerBase::isScanInProgress) || sourceCodeScannerManager.isScanInProgress();
    }

    /**
     * Create the scan logic according to Xray version.
     *
     * @return Xray scan logic
     */
    private ScanLogic createScanLogic() throws IOException {
        Logger logger = Logger.getInstance();
        ServerConfig server = GlobalSettings.getInstance().getServerConfig();
        try (XrayClient client = createXrayClientBuilder(server, logger).build()) {
            Version xrayVersion = client.system().version();
            GraphScanLogic.validateXraySupport(xrayVersion);
        }
        return new GraphScanLogic(logger);
    }
}
