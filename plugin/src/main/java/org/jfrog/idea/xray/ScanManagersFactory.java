package org.jfrog.idea.xray;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.xray.scan.GradleScanManager;
import org.jfrog.idea.xray.scan.MavenScanManager;
import org.jfrog.idea.xray.scan.NpmScanManager;
import org.jfrog.idea.xray.scan.ScanManager;
import org.jfrog.idea.xray.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Created by romang on 3/2/17.
 */
public class ScanManagersFactory {
    private Map<Class, ScanManager> scanManagers = Maps.newHashMap();

    public ScanManagersFactory(Project project) {
        setScanManagers(project);
    }

    private void setScanManagers(Project project) {
        // Create the proper scan manager according to the project type.
        if (MavenScanManager.isApplicable(project)) {
            scanManagers.put(MavenScanManager.class, new MavenScanManager(project));
        } else {
            scanManagers.remove(MavenScanManager.class);
        }

        if (GradleScanManager.isApplicable(project)) {
            scanManagers.put(GradleScanManager.class, new GradleScanManager(project));
        } else {
            scanManagers.remove(GradleScanManager.class);
        }

        Set<String> applicationsDirs = getApplicationsDirs(project);
        if (NpmScanManager.isApplicable(applicationsDirs)) {
            scanManagers.put(NpmScanManager.class, new NpmScanManager(project, applicationsDirs));
        } else {
            scanManagers.remove(NpmScanManager.class);
        }
    }

    private Set<String> getApplicationsDirs(Project project) {
        Set<Path> projectPaths = getProjectPaths(project);
        Set<String> applicationsDirs = Sets.newHashSet();
        try {
            applicationsDirs = NpmScanManager.findApplicationDirs(projectPaths);
        } catch (IOException e) {
            Utils.log("JFrog Xray npm module scan failed", Arrays.toString(e.getStackTrace()), NotificationType.ERROR);
        }
        return applicationsDirs;
    }

    private Set<Path> getProjectPaths(Project project) {
        Set<Path> projectPaths = Sets.newHashSet();
        projectPaths.add(Paths.get(ScanManager.getProjectBasePath(project)));
        scanManagers.values().forEach(scanManager -> projectPaths.addAll(scanManager.getProjectPaths()));
        return Utils.filterProjectPaths(projectPaths);
    }

    public static Set<ScanManager> getScanManagers(@NotNull Project project) {
        ScanManagersFactory scanManagersFactory = ServiceManager.getService(project, ScanManagersFactory.class);
        return Sets.newHashSet(scanManagersFactory.scanManagers.values());
    }

    public static void refreshScanManagers(@NotNull Project project) {
        ScanManagersFactory scanManagersFactory = ServiceManager.getService(project, ScanManagersFactory.class);
        scanManagersFactory.setScanManagers(project);
    }

}