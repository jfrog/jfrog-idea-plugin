package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.ide.common.persistency.XrayScanCache;
import com.jfrog.ide.common.scan.ComponentSummaryScanLogic;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.common.utils.PackageFileFinder;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.idea.scan.ScanUtils.createScanPaths;
import static com.jfrog.ide.idea.utils.Utils.*;

/**
 * Created by yahavi
 */
public class ScanManagersFactory implements Disposable {

    Map<Integer, ScanManager> scanManagers = Maps.newHashMap();
    private final MessageBusConnection busConnection;
    private final Project project;

    public static ScanManagersFactory getInstance(@NotNull Project project) {
        return project.getService(ScanManagersFactory.class);
    }

    private ScanManagersFactory(@NotNull Project project) {
        this.busConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        this.project = project;
        registerOnChangeHandlers();
    }

    public static Set<ScanManager> getScanManagers(@NotNull Project project) {
        ScanManagersFactory scanManagersFactory = getInstance(project);
        return Sets.newHashSet(scanManagersFactory.scanManagers.values());
    }

    /**
     * When the excluded paths change, scan managers should be created or deleted.
     * Therefore, we run startScan() which recreates the scan managers on refreshScanManagers().
     */
    private void registerOnChangeHandlers() {
        busConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, () -> startScan(false, true));
    }

    /**
     * Start an Xray scan for all projects.
     *
     * @param quickScan   - True to allow usage of the scan cache.
     * @param shouldToast - True to enable showing balloons logs.
     */
    public void startScan(boolean quickScan, boolean shouldToast) {
        if (DumbService.isDumb(project)) { // If intellij is still indexing the project
            return;
        }

        if (isScanInProgress()) {
            Logger.getInstance().info("Previous scan still running...");
            return;
        }

        if (!GlobalSettings.getInstance().areXrayCredentialsSet()) {
            tryConnectionDetailsFromJfrogCli();
            return;
        }

        project.getMessageBus().syncPublisher(ApplicationEvents.ON_SCAN_LOCAL_STARTED).update();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            refreshScanManagers(getScanLogicType(), executor);
            NavigationService.clearNavigationMap(project);
            for (ScanManager scanManager : scanManagers.values()) {
                try {
                    scanManager.asyncScanAndUpdateResults(quickScan, shouldToast);
                } catch (RuntimeException e) {
                    logError(Logger.getInstance(), "", e, shouldToast);
                }
            }
        } catch (IOException | RuntimeException | InterruptedException e) {
            logError(Logger.getInstance(), "", e, shouldToast);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Load connection details From JFrog CLI configuration. If credentials loaded successfully, trigger a new scan.
     */
    private void tryConnectionDetailsFromJfrogCli() {
        GlobalSettings globalSettings = GlobalSettings.getInstance();
        if (!globalSettings.loadConnectionDetailsFromJfrogCli()) {
            Logger.getInstance().warn("Xray server is not configured.");
            return;
        }
        // Send the ON_CONFIGURATION_DETAILS_CHANGE event that updates the UI panels and triggers a new Xray scan
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE).update();
    }

    /**
     * Run inspections for all scan managers.
     */
    public void runInspectionsForAllScanManagers() {
        NavigationService.clearNavigationMap(project);
        for (ScanManager scanManager : scanManagers.values()) {
            scanManager.runInspections();
        }
    }

    /**
     * Scan projects, create new ScanManagers and delete unnecessary ones.
     */
    public void refreshScanManagers(Utils.ScanLogicType scanLogicType, @Nullable ExecutorService executor) throws IOException, InterruptedException {
        removeScanManagersListeners();
        Map<Integer, ScanManager> scanManagers = Maps.newHashMap();
        int projectHash = Utils.getProjectIdentifier(project);
        ScanManager scanManager = this.scanManagers.get(projectHash);
        if (scanManager != null) {
            // Set the new executor on the old scan manager
            scanManager.setExecutor(executor);
            scanManagers.put(projectHash, scanManager);
        } else {
            // Unlike other scan managers whereby we create them if the package descriptor exist, the Maven
            // scan manager is created if the Maven plugin is installed and there are Maven projects loaded.
            createScanManagerIfApplicable(scanManagers, projectHash, ScanManagerTypes.MAVEN, "", executor);
        }
        Set<Path> scanPaths = createScanPaths(scanManagers, project);
        createScanManagers(scanManagers, scanPaths, executor);
        createPypiScanManagerIfApplicable(scanManagers, executor);
        setScanLogic(scanManagers, scanLogicType);
        this.scanManagers = scanManagers;
    }

    /**
     * Create the scan logic according to the input type.
     *
     * @param type      - GraphScan or ComponentSummary
     * @param scanCache - The scan cache
     * @param logger    - The logger
     * @return Xray scan logic
     */
    private ScanLogic createScanLogic(Utils.ScanLogicType type, ScanCache scanCache, Logger logger) {
        if (type == Utils.ScanLogicType.GraphScan) {
            return new GraphScanLogic(scanCache, logger);
        }
        return new ComponentSummaryScanLogic(scanCache, logger);
    }

    /**
     * Create npm, Gradle, and Go scan managers.
     *
     * @param scanManagers - The scan managers map including the scan manager
     *                     of the current project or an empty map, for a fresh start
     * @param scanPaths    - Potentials paths for scanning for package descriptor files
     * @param executor     - The thread pool
     * @throws IOException in case of any I/O error during the search for the actual package descriptor files.
     */
    private void createScanManagers(Map<Integer, ScanManager> scanManagers, Set<Path> scanPaths, ExecutorService executor) throws IOException {
        Path basePath = getProjectBasePath(project);
        PackageFileFinder packageFileFinder = new PackageFileFinder(scanPaths, basePath,
                GlobalSettings.getInstance().getServerConfig().getExcludedPaths(), Logger.getInstance());

        // Create yarn scan-managers.
        Set<String> yarnLockDirs = packageFileFinder.getYarnPackagesFilePairs();
        createScanManagersForPackageDirs(yarnLockDirs, scanManagers, ScanManagerTypes.YARN, executor);

        // Create npm scan-managers.
        Set<String> packageJsonDirs = packageFileFinder.getNpmPackagesFilePairs();
        createScanManagersForPackageDirs(packageJsonDirs, scanManagers, ScanManagerTypes.NPM, executor);

        // Create Gradle scan-managers.
        Set<String> buildGradleDirs = packageFileFinder.getBuildGradlePackagesFilePairs();
        createScanManagersForPackageDirs(buildGradleDirs, scanManagers, ScanManagerTypes.GRADLE, executor);

        // Create Go scan-managers.
        Set<String> goModDirs = packageFileFinder.getGoPackagesFilePairs();
        createScanManagersForPackageDirs(goModDirs, scanManagers, ScanManagerTypes.GO, executor);
    }

    /**
     * Create PypiScanManager for each Python SDK configured.
     *
     * @param scanManagers - Scan managers list
     */
    private void createPypiScanManagerIfApplicable(Map<Integer, ScanManager> scanManagers, ExecutorService executor) {
        try {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                Sdk pythonSdk = PythonSdkUtil.findPythonSdk(module);
                if (pythonSdk == null) {
                    continue;
                }
                int projectHash = Utils.getProjectIdentifier(pythonSdk.getName(), pythonSdk.getHomePath());
                ScanManager scanManager = this.scanManagers.get(projectHash);
                if (scanManager == null) {
                    scanManager = new PypiScanManager(project, pythonSdk, executor);
                }
                scanManagers.put(projectHash, scanManager);
            }
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The 'python' plugins is not installed.
        }
    }

    private void createScanManagersForPackageDirs(Set<String> packageDirs, Map<Integer, ScanManager> scanManagers,
                                                  ScanManagerTypes type, ExecutorService executor) {
        for (String dir : packageDirs) {
            int projectHash = Utils.getProjectIdentifier(dir, dir);
            ScanManager scanManager = scanManagers.get(projectHash);
            if (scanManager != null) {
                scanManagers.put(projectHash, scanManager);
            } else {
                createScanManagerIfApplicable(scanManagers, projectHash, type, dir, executor);
            }
        }
    }

    /**
     * Set the scan logic for all scan managers.
     * We create a new instance to allow setting the scan results separately after the Xray scan.
     * On the other hand, the scan cache map is a single map shared between all scanners.
     *
     * @param scanManagers - The scan managers before Xray scan
     * @throws IOException in case of any I/O error.
     */
    private void setScanLogic(Map<Integer, ScanManager> scanManagers, Utils.ScanLogicType scanLogicType) throws IOException {
        ScanCache scanCache = createXrayScanCache();
        Logger logger = Logger.getInstance();
        scanManagers.values().forEach(manager -> manager.setScanLogic(createScanLogic(scanLogicType, scanCache, logger)));
    }

    /**
     * Create the scan cache object and the directories needed for it.
     *
     * @return scan cache.
     * @throws IOException in cace of any I/O error.
     */
    private ScanCache createXrayScanCache() throws IOException {
        Files.createDirectories(HOME_PATH);
        Logger log = Logger.getInstance();
        ServerConfig server = GlobalSettings.getInstance().getServerConfig();
        return new XrayScanCache(project.getName() + server.getProject(), HOME_PATH.resolve("cache"), log);
    }

    private enum ScanManagerTypes {
        MAVEN,
        GRADLE,
        NPM,
        GO,
        YARN
    }

    /**
     * Create a new scan manager according to the scan manager type. Add it to the scan managers set.
     * Maven - Create only if the 'maven' plugin is installed and there are Maven projects.
     * Go, npm and gradle - Always create.
     *
     * @param scanManagers - Scan managers set
     * @param projectHash  - Project hash - calculated by the project name and the path
     * @param type         - Project type
     * @param dir          - Project dir
     */
    private void createScanManagerIfApplicable(Map<Integer, ScanManager> scanManagers, int projectHash, ScanManagerTypes type, String dir, ExecutorService executor) {
        try {
            ScanManager scanManager;
            switch (type) {
                case MAVEN:
                    if (MavenScanManager.isApplicable(project)) {
                        scanManager = new MavenScanManager(project, executor);
                    } else {
                        return;
                    }
                    break;
                case GRADLE:
                    scanManager = new GradleScanManager(project, dir, executor);
                    break;
                case YARN:
                    scanManager = new YarnScanManager(project, dir, executor);
                    break;
                case NPM:
                    scanManager = new NpmScanManager(project, dir, executor);
                    break;
                case GO:
                    scanManager = new GoScanManager(project, dir, executor);
                    break;
                default:
                    return;
            }
            scanManagers.put(projectHash, scanManager);
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The 'maven' or 'python' plugins are not installed.
        }
    }

    private boolean isScanInProgress() {
        return scanManagers.values().stream().anyMatch(ScanManager::isScanInProgress);
    }

    /**
     * Remove file system change listeners on each on of the scan managers.
     */
    private void removeScanManagersListeners() {
        scanManagers.values().forEach(ScanManager::dispose);
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the bus connection
        busConnection.disconnect();
    }
}
