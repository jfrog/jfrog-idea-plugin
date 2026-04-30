package com.jfrog.ide.idea.scan.utils;

import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jfrog.ide.common.utils.Utils;
import com.jfrog.ide.idea.log.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.extractor.WslUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/**
 * @author yahavi
 **/
public class ScanUtils {
    /**
     * This method gets a set of modules from IDEA, and searches for projects to be scanned.
     *
     * @param project - the project
     * @return local scan paths
     */
    public static Set<Path> createScanPaths(Project project) {
        Set<Path> paths = Sets.newHashSet();
        paths.add(com.jfrog.ide.idea.utils.Utils.getProjectBasePath(project));
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            VirtualFile modulePath = ProjectUtil.guessModuleDir(module);
            if (modulePath != null) {
                paths.add(modulePath.toNioPath());
            }
        }
        paths = Utils.consolidatePaths(paths);
        Logger.getInstance().debug("Scanning projects in the following paths: " + paths);
        return paths;
    }

    public static String getOSAndArc() throws IOException {
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

    /**
     * Extracts the WSL distro name from a Windows UNC WSL path.
     * e.g. {@code \\wsl$\Ubuntu\home\...} → {@code "Ubuntu"}
     */
    public static String extractWslDistro(String uncPath) {
        if (uncPath == null) {
            return null;
        }
        String p = WslUtils.normalizePathStringForWsl(uncPath);
        String withoutPrefix;
        if (p.regionMatches(true, 0, "\\\\wsl.localhost\\", 0, "\\\\wsl.localhost\\".length())) {
            withoutPrefix = p.substring("\\\\wsl.localhost\\".length());
        } else if (p.regionMatches(true, 0, "\\\\wsl$\\", 0, "\\\\wsl$\\".length())) {
            withoutPrefix = p.substring("\\\\wsl$\\".length());
        } else {
            return null;
        }
        int sep = withoutPrefix.indexOf('\\');
        return sep == -1 ? withoutPrefix : withoutPrefix.substring(0, sep);
    }

    /**
     * Returns the home directory of the default user inside the given WSL distro by running
     * {@code wsl.exe -d <distro> -e printenv HOME}.
     *
     * @throws IOException if wsl.exe cannot be executed or returns a non-zero exit code
     */
    public static String getWslLinuxHome(String distro) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("wsl.exe", "-d", distro, "-e", "printenv", "HOME");
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.readLine();
        }
        int exit;
        try {
            exit = proc.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while querying WSL home directory", e);
        }
        if (exit != 0 || StringUtils.isBlank(output)) {
            throw new IOException(String.format("Failed to determine WSL home directory for distro '%s' (exit %d)", distro, exit));
        }
        return output.trim();
    }

    /**
     * Returns the OS/arch download token for the given WSL distro (e.g. {@code "linux-amd64"})
     * by running {@code wsl.exe -d <distro> -e uname -m}. Falls back to {@code "linux-amd64"}
     * on any error.
     */
    public static String getWslArch(String distro) {
        try {
            ProcessBuilder pb = new ProcessBuilder("wsl.exe", "-d", distro, "-e", "uname", "-m");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String arch;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                arch = reader.readLine();
            }
            proc.waitFor();
            if (StringUtils.isBlank(arch)) {
                return "linux-amd64";
            }
            return switch (arch.trim()) {
                case "aarch64", "arm64" -> "linux-arm64";
                case "armv7l", "arm" -> "linux-arm";
                default -> "linux-amd64";
            };
        } catch (Exception e) {
            return "linux-amd64";
        }
    }
}
