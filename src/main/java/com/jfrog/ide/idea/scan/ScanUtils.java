package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jfrog.ide.idea.log.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
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

    static String getOSAndArc() throws IOException {
        String arch = SystemUtils.OS_ARCH;
        // Windows
        if (SystemUtils.IS_OS_WINDOWS) {
            return "windows-amd64";
        }
        // Mac
        if (SystemUtils.IS_OS_MAC) {
            if (StringUtils.equalsAny(arch, "aarch64", "arm64")) {
                return "mac-arm64";
            } else {
                return "mac-amd64";
            }
        }
        // Linux
        if (SystemUtils.IS_OS_LINUX) {
            switch (arch) {
                case "i386":
                case "i486":
                case "i586":
                case "i686":
                case "i786":
                case "x86":
                    return "linux-386";
                case "amd64":
                case "x86_64":
                case "x64":
                    return "linux-amd64";
                case "arm":
                case "armv7l":
                    return "linux-arm";
                case "aarch64":
                    return "linux-arm64";
                case "ppc64":
                case "ppc64le":
                    return "linux-" + arch;
            }
        }
        throw new IOException(String.format("Unsupported OS: %s-%s", SystemUtils.OS_NAME, arch));
    }
}
