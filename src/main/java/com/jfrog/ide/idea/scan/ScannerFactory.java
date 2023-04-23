package com.jfrog.ide.idea.scan;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.common.utils.PackageFileFinder;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.log.Logger;
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
     * Scan projects, create new Scanners and delete unnecessary ones.
     * Existing Scanners from previous scans, are not overridden.
     */
    public Map<Integer, ScannerBase> refreshScanners(Map<Integer, ScannerBase> oldScanners, ScanLogic scanLogic,
                                                     @Nullable ExecutorService executor) throws IOException {
        Map<Integer, ScannerBase> scanners = Maps.newHashMap();
        refreshMavenScanner(scanners, oldScanners, executor, scanLogic);
        refreshPypiScanners(scanners, oldScanners, executor, scanLogic);
        Set<Path> scanPaths = createScanPaths(oldScanners, project);
        refreshGenericScanners(scanners, oldScanners, scanPaths, executor, scanLogic);
        return scanners;
    }

    /**
     * Create npm, Gradle, Go and Yarn Scanners.
     *
     * @param newScanners the new Scanners map to add the Scanners into
     * @param oldScanners the Scanners map including the Scanner of the current project, or an empty map for a fresh start
     * @param scanPaths   potential paths for scanning for package descriptor files
     * @param executor    the thread pool
     * @throws IOException in case of any I/O error during the search for the actual package descriptor files.
     */
    private void refreshGenericScanners(Map<Integer, ScannerBase> newScanners, Map<Integer, ScannerBase> oldScanners,
                                        Set<Path> scanPaths, ExecutorService executor, ScanLogic scanLogic) throws IOException {
        Path basePath = getProjectBasePath(project);
        PackageFileFinder packageFileFinder = new PackageFileFinder(scanPaths, basePath,
                GlobalSettings.getInstance().getServerConfig().getExcludedPaths(), Logger.getInstance());

        // Create Yarn scanners
        Set<String> yarnLockDirs = packageFileFinder.getYarnPackagesFilePairs();
        refreshGenericScannersByType(yarnLockDirs, newScanners, oldScanners, GenericScannerType.YARN, executor, scanLogic);

        // Create npm scanners
        Set<String> packageJsonDirs = packageFileFinder.getNpmPackagesFilePairs();
        refreshGenericScannersByType(packageJsonDirs, newScanners, oldScanners, GenericScannerType.NPM, executor, scanLogic);

        // Create Gradle scanners
        Set<String> buildGradleDirs = packageFileFinder.getBuildGradlePackagesFilePairs();
        refreshGenericScannersByType(buildGradleDirs, newScanners, oldScanners, GenericScannerType.GRADLE, executor, scanLogic);

        // Create Go scanners
        Set<String> goModDirs = packageFileFinder.getGoPackagesFilePairs();
        refreshGenericScannersByType(goModDirs, newScanners, oldScanners, GenericScannerType.GO, executor, scanLogic);
    }

    /**
     * Create a MavenScanner if this is a Maven project.
     *
     * @param newScanners new scanners map
     * @param oldScanners existing scanners map
     * @param executor    an executor that should limit the number of running tasks to 3
     * @param scanLogic   the scan logic to use
     */
    private void refreshMavenScanner(Map<Integer, ScannerBase> newScanners, Map<Integer, ScannerBase> oldScanners,
                                     ExecutorService executor, ScanLogic scanLogic) {
        int projectHash = getModuleIdentifier(project.getName(), project.getBasePath());
        ScannerBase scanner = oldScanners.get(projectHash);

        // Check if a ScanManager for this project already exists
        if (scanner != null) {
            // Set the new executor on the old scan manager
            scanner.setExecutor(executor);
            scanner.setScanLogic(scanLogic);
            newScanners.put(projectHash, scanner);
        } else {
            // Unlike other scanners whereby we create them if the package descriptor exist, MavenScanner is created if
            // the Maven plugin is installed and there are Maven projects loaded.
            try {
                if (MavenScanner.isApplicable(project)) {
                    scanner = new MavenScanner(project, executor, scanLogic);
                    newScanners.put(projectHash, scanner);
                }
            } catch (NoClassDefFoundError noClassDefFoundError) {
                // The Maven plugin is not installed.
            }
        }
    }

    /**
     * Create PypiScanner for each module with Python SDK configured.
     *
     * @param newScanners new scanners map
     * @param oldScanners existing scanners map
     * @param executor    an executor that should limit the number of running tasks to 3
     * @param scanLogic   the scan logic to use
     */
    private void refreshPypiScanners(Map<Integer, ScannerBase> newScanners, Map<Integer, ScannerBase> oldScanners,
                                     ExecutorService executor, ScanLogic scanLogic) {
        try {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                Sdk pythonSdk = PythonSdkUtil.findPythonSdk(module);
                if (pythonSdk == null) {
                    continue;
                }
                int projectHash = getModuleIdentifier(pythonSdk.getName(), pythonSdk.getHomePath());
                ScannerBase scanner = oldScanners.get(projectHash);
                if (scanner == null) {
                    scanner = new PypiScanner(project, pythonSdk, executor, scanLogic);
                }
                scanner.setExecutor(executor);
                scanner.setScanLogic(scanLogic);
                newScanners.put(projectHash, scanner);
            }
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The Python plugin is not installed.
        }
    }

    private void refreshGenericScannersByType(Set<String> packageDirs, Map<Integer, ScannerBase> newScanners, Map<Integer, ScannerBase> oldScanners,
                                              GenericScannerType type, ExecutorService executor, ScanLogic scanLogic) {
        for (String dir : packageDirs) {
            int projectHash = getModuleIdentifier(dir, dir);
            ScannerBase scanner = oldScanners.get(projectHash);
            if (scanner == null) {
                scanner = createGenericScanner(type, dir, executor, scanLogic);
            }
            if (scanner != null) {
                scanner.setExecutor(executor);
                scanner.setScanLogic(scanLogic);
                newScanners.put(projectHash, scanner);
            }
        }
    }

    /**
     * Create a new scanner according to the type. Add it to the scanners map.
     * Supported types: Go, npm, gradle and Yarn.
     *
     * @param type      project type
     * @param dir       project dir
     * @param executor  an executor that should limit the number of running tasks to 3
     * @param scanLogic the scan logic to use
     */
    private ScannerBase createGenericScanner(GenericScannerType type, String dir, ExecutorService executor, ScanLogic scanLogic) {
        try {
            switch (type) {
                case GRADLE:
                    return new GradleScanner(project, dir, executor, scanLogic);
                case YARN:
                    return new YarnScanner(project, dir, executor, scanLogic);
                case NPM:
                    return new NpmScanner(project, dir, executor, scanLogic);
                case GO:
                    return new GoScanner(project, dir, executor, scanLogic);
            }
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // The Gradle plugin is not installed.
        }
        return null;
    }

    private enum GenericScannerType {
        GRADLE,
        NPM,
        GO,
        YARN
    }
}
