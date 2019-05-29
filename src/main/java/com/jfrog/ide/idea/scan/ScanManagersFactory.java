package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.jfrog.ide.idea.NpmProject;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.issues.IssuesTree;
import com.jfrog.ide.idea.ui.licenses.LicensesTree;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.jfrog.ide.common.utils.Utils.findPackageJsonDirs;

/**
 * Created by yahavi
 */
public class ScanManagersFactory {

    private Map<Integer, ScanManager> scanManagers = Maps.newHashMap();

    public static ScanManagersFactory getInstance() {
        return ServiceManager.getService(ScanManagersFactory.class);
    }

    private ScanManagersFactory() {
    }

    public static Set<ScanManager> getScanManagers() {
        ScanManagersFactory scanManagersFactory = ServiceManager.getService(ScanManagersFactory.class);
        return Sets.newHashSet(scanManagersFactory.scanManagers.values());
    }

    /**
     * Start an Xray scan for all projects.
     *
     * @param quickScan           - True to allow usage of the scan cache.
     * @param libraryDependencies - Dependencies to use in Gradle scans.
     * @param modelsProvider      - Modules to use in Gradle scans.
     */
    public void startScan(boolean quickScan, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies, @Nullable IdeModifiableModelsProvider modelsProvider) {
        if (isScanInProgress()) {
            Logger.getInstance().info("Previous scan still running...");
            return;
        }
        if (!GlobalSettings.getInstance().areCredentialsSet()) {
            Logger.getInstance().error("Xray server is not configured.");
            return;
        }
        try {
            IssuesTree issuesTree = IssuesTree.getInstance();
            LicensesTree licensesTree = LicensesTree.getInstance();
            if (issuesTree == null || licensesTree == null) {
                return;
            }
            refreshScanManagers();
            resetViews(issuesTree, licensesTree);
            for (ScanManager scanManager : scanManagers.values()) {
                scanManager.asyncScanAndUpdateResults(quickScan, libraryDependencies, modelsProvider);
            }
        } catch (IOException | RuntimeException e) {
            Logger.getInstance().error("", e);
        }
    }

    /**
     * Start an Xray scan after Gradle dependencies import.
     * For known Gradle projects - Start scan only for the project.
     * For new Gradle projects - Start a full scan.
     *
     * @param project             - The Gradle project
     * @param libraryDependencies - Gradle's dependencies
     * @param modelsProvider      - Gradle's modules
     */
    public void tryScanSingleProject(Project project, Collection<DataNode<LibraryDependencyData>> libraryDependencies, IdeModifiableModelsProvider modelsProvider) {
        ScanManager scanManager = scanManagers.get(Utils.getProjectIdentifier(project));
        if (scanManager != null) { // If Gradle project already exists
            scanManager.asyncScanAndUpdateResults(true, libraryDependencies, modelsProvider);
            return;
        }
        startScan(true, libraryDependencies, modelsProvider); // New Gradle project
    }

    /**
     * Scan for Maven, Gradle and Npm projects. Create new ScanManagers and delete unnecessary ones.
     */
    public void refreshScanManagers() throws IOException {
        Map<Integer, ScanManager> scanManagers = Maps.newHashMap();
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (ArrayUtils.isEmpty(projects)) {
            return;
        }
        final Set<Path> paths = Sets.newHashSet();
        for (Project project : projects) {
            int projectHash = Utils.getProjectIdentifier(project);
            ScanManager scanManager = this.scanManagers.get(projectHash);
            if (scanManager != null) {
                scanManagers.put(projectHash, scanManager);
            } else {
                if (MavenScanManager.isApplicable(project)) {
                    scanManagers.put(projectHash, new MavenScanManager(project));
                }
                if (GradleScanManager.isApplicable(project)) {
                    scanManagers.put(projectHash, new GradleScanManager(project));
                }
            }
            paths.add(Utils.getProjectBasePath(project));
        }
        createNpmScanManagers(scanManagers, paths);
        this.scanManagers = scanManagers;
    }

    private void createNpmScanManagers(Map<Integer, ScanManager> scanManagers, Set<Path> paths) throws IOException {
        scanManagers.values().stream().map(ScanManager::getProjectPaths).flatMap(Collection::stream).forEach(paths::add);
        Set<String> packageJsonDirs = findPackageJsonDirs(paths);
        for (String dir : packageJsonDirs) {
            int projectHash = Utils.getProjectIdentifier(dir, dir);
            ScanManager scanManager = this.scanManagers.get(projectHash);
            if (scanManager != null) {
                scanManagers.put(projectHash, scanManager);
            } else {
                scanManagers.put(projectHash, new NpmScanManager(new NpmProject(dir)));
            }
        }
    }

    private boolean isScanInProgress() {
        return scanManagers.values().stream().anyMatch(ScanManager::isScanInProgress);
    }

    private void resetViews(IssuesTree issuesTree, LicensesTree licensesTree) {
        issuesTree.reset();
        licensesTree.reset();
    }
}