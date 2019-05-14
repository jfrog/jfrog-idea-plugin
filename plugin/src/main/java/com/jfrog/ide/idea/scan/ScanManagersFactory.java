package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.issues.IssuesTree;
import com.jfrog.ide.idea.ui.licenses.LicensesTree;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static com.jfrog.ide.common.utils.Utils.findPackageJsonDirs;

/**
 * Created by romang on 3/2/17.
 */
public class ScanManagersFactory {

    private List<ScanManager> scanManagers = Lists.newArrayList();

    public static ScanManagersFactory getInstance() {
        return ServiceManager.getService(ScanManagersFactory.class);
    }

    private ScanManagersFactory() {
    }

    public static Set<ScanManager> getScanManagers() {
        ScanManagersFactory scanManagersFactory = ServiceManager.getService(ScanManagersFactory.class);
        return Sets.newHashSet(scanManagersFactory.scanManagers);
    }

    public void startScan(boolean quickScan) throws IOException {
        if (isScanInProgress()) {
            Logger.getInstance().info("Previous scan still running...");
            return;
        }
        refreshScanManagers();
        IssuesTree issuesTree = IssuesTree.getInstance();
        LicensesTree licensesTree = LicensesTree.getInstance();
        if (issuesTree == null || licensesTree == null) {
            return;
        }
        resetViews(issuesTree, licensesTree);
        for (ScanManager scanManager : scanManagers) {
            scanManager.asyncScanAndUpdateResults(quickScan);
        }
    }

    private void refreshScanManagers() throws IOException {
        scanManagers = Lists.newArrayList();
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (ArrayUtils.isEmpty(projects)) {
            return;
        }
        Set<Path> paths = Sets.newHashSet();
        for (Project project : projects) {
            if (MavenScanManager.isApplicable(project)) {
                scanManagers.add(new MavenScanManager(project));
            }

            if (GradleScanManager.isApplicable(project)) {
                scanManagers.add(new GradleScanManager(project));
            }
            paths.add(Paths.get(project.getBasePath()));
        }
        Set<String> packageJsonDirs = findPackageJsonDirs(paths);
        for (String dir : packageJsonDirs) {
            Project npmProject = ProjectManager.getInstance().createProject(dir, dir);
            scanManagers.add(new NpmScanManager(npmProject));
        }
    }

    private boolean isScanInProgress() {
        return scanManagers.stream().anyMatch(ScanManager::isScanInProgress);
    }


    private void resetViews(IssuesTree issuesTree, LicensesTree licensesTree) {
        issuesTree.reset();
        licensesTree.reset();
    }


}