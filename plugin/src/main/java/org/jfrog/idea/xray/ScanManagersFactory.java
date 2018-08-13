package org.jfrog.idea.xray;

import com.google.common.collect.Lists;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.xray.scan.GradleScanManager;
import org.jfrog.idea.xray.scan.MavenScanManager;
import org.jfrog.idea.xray.scan.NpmScanManager;
import org.jfrog.idea.xray.scan.ScanManager;

import java.util.List;

/**
 * Created by romang on 3/2/17.
 */
public class ScanManagersFactory {
    private List<ScanManager> scanManagers;

    public ScanManagersFactory(Project project) {
        initScanManagers(project);
    }

    public void initScanManagers(Project project) {
        scanManagers = Lists.newArrayList();
        // create the proper scan manager according to the project type.
        if (NpmScanManager.isApplicable(project)) {
            scanManagers.add(new NpmScanManager(project));
        }
        if (MavenScanManager.isApplicable(project)) {
            scanManagers.add(new MavenScanManager(project));
        }
        if (GradleScanManager.isApplicable(project)) {
            scanManagers.add( new GradleScanManager(project));
        }
    }

    public static List<ScanManager> getScanManagers(@NotNull Project project) {
        ScanManagersFactory scanManagersFactory = ServiceManager.getService(project, ScanManagersFactory.class);
        return scanManagersFactory.scanManagers;
    }
}