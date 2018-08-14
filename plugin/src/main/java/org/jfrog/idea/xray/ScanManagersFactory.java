package org.jfrog.idea.xray;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
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
import java.util.*;

/**
 * Created by romang on 3/2/17.
 */
public class ScanManagersFactory {
    private Set<ScanManager> scanManagers;
    private static final Logger logger = Logger.getInstance(ScanManagersFactory.class);

    public ScanManagersFactory(Project project) {
        initScanManagers(project);
    }

    public void initScanManagers(Project project) {
        scanManagers = new HashSet<>();
        setScanManagers(project);
    }

    private void setScanManagers(Project project) {
        // create the proper scan manager according to the project type.
        if (MavenScanManager.isApplicable(project)) {
            scanManagers.add(new MavenScanManager(project));
        } else {
            scanManagers.remove(new MavenScanManager(project));
        }

        if (GradleScanManager.isApplicable(project)) {
            scanManagers.add(new GradleScanManager(project));
        } else {
            scanManagers.remove(new GradleScanManager(project));
        }
        scanManagers.remove(new NpmScanManager());
        addNpmScannerIfNeeded(project);
    }

    private void addNpmScannerIfNeeded(Project project) {
        Set<Path> projectPaths = new HashSet<>();
        projectPaths.add(Paths.get(ScanManager.getProjectBasePath(project)));
        scanManagers.forEach(scanManager -> projectPaths.addAll(scanManager.getProjectPaths()));
        Set<Path> finalProjectPaths = Utils.filterProjectPaths(projectPaths);
        Set<String> applicationsDirs;
        try {
            applicationsDirs = NpmScanManager.findApplicationDirs(finalProjectPaths);
        } catch (IOException e) {
            Utils.log(logger, "JFrog Xray npm module scan failed", Arrays.toString(e.getStackTrace()), NotificationType.ERROR);
            return;
        }
        if (NpmScanManager.isApplicable(applicationsDirs)) {
            NpmScanManager npmScanManager = new NpmScanManager(project, applicationsDirs);
            scanManagers.add(npmScanManager);
        }
    }

    public static Set<ScanManager> getScanManagers(@NotNull Project project) {
        ScanManagersFactory scanManagersFactory = ServiceManager.getService(project, ScanManagersFactory.class);
        return scanManagersFactory.scanManagers;
    }

    public static void refreshScanManagers(@NotNull Project project) {
        ScanManagersFactory scanManagersFactory = ServiceManager.getService(project, ScanManagersFactory.class);
        scanManagersFactory.setScanManagers(project);
    }


}