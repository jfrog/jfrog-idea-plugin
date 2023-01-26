package com.jfrog.ide.idea.scan;

import com.google.common.base.Objects;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.common.utils.PackageFileFinder;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.jfrog.ide.idea.scan.ScanUtils.createScanPaths;
import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;

/**
 * Created by yahavi
 */
public class ScannerFactory {
    private final Project project;

    public ScannerFactory(Project project) {
        this.project = project;
    }

    public static int getModuleIdentifier(String name, String path) {
        return Objects.hashCode(name, path);
    }

    /**
     * Create the scan logic according to the input type.
     *
     * @param type    - the Xray scan type
     * @param pkgType - package type name
     * @param logger  - logger
     * @return Xray scan logic
     */
    private ScanLogic createScanLogic(Utils.ScanLogicType type, String pkgType, Logger logger) {
        return new GraphScanLogic(pkgType, logger);
    }

    /**
     * Scan projects, create new Scanners and delete unnecessary ones.
     */
    public void refreshScanners(Map<Integer, ScannerBase> scanners, Utils.ScanLogicType scanLogicType,
                                @Nullable ExecutorService executor) throws IOException {
        createMavenScannerIfApplicable(scanners, executor);
        createPypiScanners(scanners, executor);
        Set<Path> scanPaths = createScanPaths(scanners, project);
        createGenericScanners(scanners, scanPaths, executor);
        setScanLogic(scanners, scanLogicType);
    }

    /**
     * Create npm, Gradle, Go and Yarn scanners.
     *
     * @param scannersMap - The scanners map including the scanner of the current project, or an empty map for a fresh start
     * @param scanPaths   - Potentials paths for scanning for package descriptor files
     * @param executor    - The thread pool
     * @throws IOException in case of any I/O error during the search for the actual package descriptor files.
     */
    private void createGenericScanners(Map<Integer, ScannerBase> scannersMap, Set<Path> scanPaths, ExecutorService executor) throws IOException {
        Path basePath = getProjectBasePath(project);
        PackageFileFinder packageFileFinder = new PackageFileFinder(scanPaths, basePath,
                GlobalSettings.getInstance().getServerConfig().getExcludedPaths(), Logger.getInstance());

        // Create Yarn scanners
        Set<String> yarnLockDirs = packageFileFinder.getYarnPackagesFilePairs();
        createGenericScanners(yarnLockDirs, scannersMap, GenericScannerType.YARN, executor);

        // Create npm scanners
        Set<String> packageJsonDirs = packageFileFinder.getNpmPackagesFilePairs();
        createGenericScanners(packageJsonDirs, scannersMap, GenericScannerType.NPM, executor);

        // Create Gradle scanners
        Set<String> buildGradleDirs = packageFileFinder.getBuildGradlePackagesFilePairs();
        createGenericScanners(buildGradleDirs, scannersMap, GenericScannerType.GRADLE, executor);

        // Create Go scanners
        Set<String> goModDirs = packageFileFinder.getGoPackagesFilePairs();
        createGenericScanners(goModDirs, scannersMap, GenericScannerType.GO, executor);
    }

    /**
     * Create a MavenScanner if this is a Maven project.
     *
     * @param scannersMap - scanners map
     */
    private void createMavenScannerIfApplicable(Map<Integer, ScannerBase> scannersMap, ExecutorService executor) {
        int projectHash = getModuleIdentifier(project.getName(), project.getBasePath());
        ScannerBase scanner = scannersMap.get(projectHash);

        // Check if a ScanManager for this project already exists
        if (scanner != null) {
            // Set the new executor on the old scan manager
            scanner.setExecutor(executor);
            scannersMap.put(projectHash, scanner);
        } else {
            // Unlike other scanners whereby we create them if the package descriptor exist, MavenScanner is created if
            // the Maven plugin is installed and there are Maven projects loaded.
            try {
                if (MavenScanner.isApplicable(project)) {
                    scanner = new MavenScanner(project, executor);
                    scannersMap.put(projectHash, scanner);
                }
            } catch (NoClassDefFoundError noClassDefFoundError) {
                // The Maven plugin is not installed.
            }
        }
    }

    /**
     * Create PypiScanner for each module with Python SDK configured.
     *
     * @param scannersMap - scanners map
     */
    private void createPypiScanners(Map<Integer, ScannerBase> scannersMap, ExecutorService executor) {
        try {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                Sdk pythonSdk = PythonSdkUtil.findPythonSdk(module);
                if (pythonSdk == null) {
                    continue;
                }
                int projectHash = getModuleIdentifier(pythonSdk.getName(), pythonSdk.getHomePath());
                ScannerBase scanner = scannersMap.get(projectHash);
                if (scanner == null) {
                    scanner = new PypiScanner(project, pythonSdk, executor);
                }
                scannersMap.put(projectHash, scanner);
            }
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The Python plugin is not installed.
        }
    }

    private void createGenericScanners(Set<String> packageDirs, Map<Integer, ScannerBase> scannersMap,
                                       GenericScannerType type, ExecutorService executor) {
        for (String dir : packageDirs) {
            int projectHash = getModuleIdentifier(dir, dir);
            ScannerBase scanner = scannersMap.get(projectHash);
            if (scanner == null) {
                createGenericScannerByType(scannersMap, projectHash, type, dir, executor);
            }
        }
    }

    /**
     * Set the scan logic for all scanners.
     *
     * @param scannersMap - the scanners before Xray scan
     */
    private void setScanLogic(Map<Integer, ScannerBase> scannersMap, Utils.ScanLogicType scanLogicType) {
        Logger logger = Logger.getInstance();
        scannersMap.values().forEach(manager -> manager.setScanLogic(createScanLogic(scanLogicType, manager.getPackageType(), logger)));
    }

    /**
     * Create a new scanner according to the type. Add it to the scanners map.
     * Supported types: Go, npm, gradle and Yarn.
     *
     * @param scannersMap - scanners map
     * @param projectHash - project hash - calculated by the project name and the path
     * @param type        - project type
     * @param dir         - project dir
     */
    private void createGenericScannerByType(Map<Integer, ScannerBase> scannersMap, int projectHash, GenericScannerType type, String dir, ExecutorService executor) {
        try {
            ScannerBase scanner;
            switch (type) {
                case GRADLE:
                    scanner = new GradleScanner(project, dir, executor);
                    break;
                case YARN:
                    scanner = new YarnScanner(project, dir, executor);
                    break;
                case NPM:
                    scanner = new NpmScanner(project, dir, executor);
                    break;
                case GO:
                    scanner = new GoScanner(project, dir, executor);
                    break;
                default:
                    return;
            }
            scannersMap.put(projectHash, scanner);
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The Gradle plugin is not installed.
        }
    }

    private enum GenericScannerType {
        GRADLE,
        NPM,
        GO,
        YARN
    }
}
