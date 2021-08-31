package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jfrog.ide.common.persistency.XrayScanCache;
import com.jfrog.ide.common.scan.BulkScanLogic;
import com.jfrog.ide.common.scan.EmptyScanLogic;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.common.utils.PackageFileFinder;
import com.jfrog.ide.common.utils.XrayConnectionUtils;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.utils.Utils;
import com.jfrog.xray.client.impl.XrayClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;

/**
 * Created by yahavi
 */
public class ScanManagersFactory {

    private static final Path HOME_PATH = Paths.get(System.getProperty("user.home"), ".jfrog-idea-plugin");
    private Map<Integer, ScanManager> scanManagers = Maps.newHashMap();
    private final Project project;

    public static ScanManagersFactory getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, ScanManagersFactory.class);
    }

    private ScanManagersFactory(@NotNull Project project) {
        try {
            Files.createDirectories(HOME_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.project = project;
    }

    public static Set<ScanManager> getScanManagers(@NotNull Project project) {
        ScanManagersFactory scanManagersFactory = getInstance(project);
        return Sets.newHashSet(scanManagersFactory.scanManagers.values());
    }

    /**
     * Start an Xray scan for all projects.
     *
     * @param quickScan - True to allow usage of the scan cache.
     */
    public void startScan(boolean quickScan) {
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
        try {
            ComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
            if (componentsTree == null) {
                return;
            }
            refreshScanManagers();
            componentsTree.reset();
            NavigationService.clearNavigationMap(project);
            for (ScanManager scanManager : scanManagers.values()) {
                try {
                    scanManager.asyncScanAndUpdateResults(quickScan);
                } catch (RuntimeException e) {
                    logError(Logger.getInstance(), "", e, !quickScan);
                }
            }
        } catch (IOException | RuntimeException e) {
            logError(Logger.getInstance(), "", e, !quickScan);
        }
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
    public void refreshScanManagers() throws IOException {
        Map<Integer, ScanManager> scanManagers = Maps.newHashMap();
        final Set<Path> paths = Sets.newHashSet();
        int projectHash = Utils.getProjectIdentifier(project);
        ScanManager scanManager = this.scanManagers.get(projectHash);
        ScanLogic logic = createScanLogic();
        if (scanManager != null) {
            scanManagers.put(projectHash, scanManager);
        } else {
            // Unlike other scan managers whereby we create them if the package descriptor exist, the Maven
            // scan manager is created if the Maven plugin is installed and there are Maven projects loaded.
            createScanManagerIfApplicable(scanManagers, projectHash, ScanManagerTypes.MAVEN, "", logic);
        }
        paths.add(Utils.getProjectBasePath(project));
        createScanManagers(scanManagers, paths, logic);
        createPypiScanManagerIfApplicable(scanManagers, logic);
        this.scanManagers = scanManagers;
    }

    private ScanLogic createScanLogic() {
        Logger log =Logger.getInstance();
        XrayClient client = createXrayClientBuilder(GlobalSettings.getInstance().getServerConfig(), log).build();
        try {
            XrayScanCache scanCache = new XrayScanCache(project.getName(), HOME_PATH.resolve("cache"), log);

            if (GraphScanLogic.isXrayVersionSupported(client.system().version())) {
                return new GraphScanLogic();
            } else {
                if (BulkScanLogic.isXrayVersionSupported(client.system().version())) {
                    return new BulkScanLogic(scanCache, log);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new EmptyScanLogic(log);
    }

    private void createScanManagers(Map<Integer, ScanManager> scanManagers, Set<Path> paths, ScanLogic logic) throws IOException {
        scanManagers.values().stream().map(ScanManager::getProjectPaths).flatMap(Collection::stream).forEach(paths::add);
        PackageFileFinder packageFileFinder = new PackageFileFinder(paths, GlobalSettings.getInstance().getServerConfig().getExcludedPaths(), Logger.getInstance());

        // Create npm scan-managers.
        Set<String> packageJsonDirs = packageFileFinder.getNpmPackagesFilePairs();
        createScanManagersForPackageDirs(packageJsonDirs, scanManagers, ScanManagerTypes.NPM, logic);

        // Create Gradle scan-managers.
        Set<String> buildGradleDirs = packageFileFinder.getBuildGradlePackagesFilePairs();
        createScanManagersForPackageDirs(buildGradleDirs, scanManagers, ScanManagerTypes.GRADLE, logic);

        // Create Go scan-managers.
        Set<String> goModDirs = packageFileFinder.getGoPackagesFilePairs();
        createScanManagersForPackageDirs(goModDirs, scanManagers, ScanManagerTypes.GO, logic);
    }

    /**
     * Create PypiScanManager for each Python SDK configured.
     *
     * @param scanManagers - The scan managers list
     */
    private void createPypiScanManagerIfApplicable(Map<Integer, ScanManager> scanManagers, ScanLogic logic) throws IOException {
        try {
            for (Sdk pythonSdk : PypiScanManager.getAllPythonSdks()) {
                int projectHash = Utils.getProjectIdentifier(pythonSdk.getName(), pythonSdk.getHomePath());
                ScanManager scanManager = this.scanManagers.get(projectHash);
                if (scanManager == null) {
                    scanManager = new PypiScanManager(project, pythonSdk, logic);
                }
                scanManagers.put(projectHash, scanManager);
            }
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The 'python' plugins is not installed.
        }
    }

    private void createScanManagersForPackageDirs(Set<String> packageDirs, Map<Integer, ScanManager> scanManagers,
                                                  ScanManagerTypes type, ScanLogic logic) throws IOException {
        for (String dir : packageDirs) {
            int projectHash = Utils.getProjectIdentifier(dir, dir);
            ScanManager scanManager = scanManagers.get(projectHash);
            if (scanManager != null) {
                scanManagers.put(projectHash, scanManager);
            } else {
                createScanManagerIfApplicable(scanManagers, projectHash, type, dir, logic);
            }
        }
    }

    private enum ScanManagerTypes {
        MAVEN,
        GRADLE,
        NPM,
        GO
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
     * @throws IOException in any case of error during scan manager creation.
     */
    private void createScanManagerIfApplicable(Map<Integer, ScanManager> scanManagers, int projectHash, ScanManagerTypes type, String dir, ScanLogic scanLogic) throws IOException {
        try {
            switch (type) {
                case MAVEN:
                    if (MavenScanManager.isApplicable(project)) {
                        scanManagers.put(projectHash, new MavenScanManager(project, scanLogic));
                    }
                    return;
                case GRADLE:
                    scanManagers.put(projectHash, new GradleScanManager(project, dir, scanLogic));
                    return;
                case NPM:
                    scanManagers.put(projectHash, new NpmScanManager(project, dir, scanLogic));
                    return;
                case GO:
                    scanManagers.put(projectHash, new GoScanManager(project, dir, scanLogic));
            }
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The 'maven' or 'python' plugins are not installed.
        }
    }

    private boolean isScanInProgress() {
        return scanManagers.values().stream().anyMatch(ScanManager::isScanInProgress);
    }
}
