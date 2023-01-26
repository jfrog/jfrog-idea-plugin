package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jfrog.ide.common.utils.PackageFileFinder;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.log.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * @author yahavi
 **/
public class ScanUtils {
    /**
     * Return true if there is a supported project under the workspace.
     *
     * @param project - The project
     * @return true if there is a supported project under the workspace.
     * @throws IOException in case or any I/O error in PackageFileFinder.
     */
    public static boolean isLocalProjectSupported(Project project) throws IOException {
        // Check if Maven project exist
        try {
            if (MavenScanner.isApplicable(project)) {
                return true;
            }
        } catch (NoClassDefFoundError ignore) {
            // Maven plugin is not installed
        }
        // Check if Pypi SDK exist
        try {
            if (!PypiScanner.getAllPythonSdks().isEmpty()) {
                return true;
            }
        } catch (NoClassDefFoundError ignore) {
            // Maven plugin is not installed
        }

        // Check if npm, Gradle, or Go projects exist
        Set<Path> scanPaths = createScanPaths(Maps.newHashMap(), project);
        Path basePath = getProjectBasePath(project);
        PackageFileFinder packageFileFinder = new PackageFileFinder(scanPaths, basePath, GlobalSettings.getInstance().getServerConfig().getExcludedPaths(), Logger.getInstance());
        return isNotEmpty(packageFileFinder.getNpmPackagesFilePairs()) ||
                isNotEmpty(packageFileFinder.getBuildGradlePackagesFilePairs()) ||
                isNotEmpty(packageFileFinder.getGoPackagesFilePairs());
    }

    /**
     * This method gets a set of modules from IDEA, and searches for projects to be scanned.
     * It appends the root path of each module it finds into a set.
     *
     * @param scannersMap - current scanners
     * @param project     - the project
     * @return local scan paths
     */
    static Set<Path> createScanPaths(Map<Integer, ScannerBase> scannersMap, Project project) {
        final Set<Path> paths = Sets.newHashSet();
        paths.add(com.jfrog.ide.idea.utils.Utils.getProjectBasePath(project));
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            VirtualFile modulePath = ProjectUtil.guessModuleDir(module);
            if (modulePath != null) {
                paths.add(modulePath.toNioPath());
            }
        }
        scannersMap.values().stream().map(ScannerBase::getProjectPaths).flatMap(Collection::stream).forEach(paths::add);
        Logger.getInstance().debug("Scanning projects in the following paths: " + paths);
        return paths;
    }
}
