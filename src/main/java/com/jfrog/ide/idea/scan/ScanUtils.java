package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jfrog.ide.idea.log.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author yahavi
 **/
public class ScanUtils {
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
