package org.jfrog.idea.xray;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.xray.scan.GradleScanManager;
import org.jfrog.idea.xray.scan.MavenScanManager;
import org.jfrog.idea.xray.scan.NpmScanManager;
import org.jfrog.idea.xray.scan.ScanManager;

/**
 * Created by romang on 3/2/17.
 */
public class ScanManagerFactory {
    private ScanManager scanManager;

    public ScanManagerFactory(Project project) {
        initScanManager(project);
    }

    public void initScanManager(Project project) {
        // create the proper scan manager according to the project type.
        if (MavenScanManager.isApplicable(project)) {
            scanManager = new MavenScanManager(project);
            return;
        }
        if (GradleScanManager.isApplicable(project)) {
            scanManager = new GradleScanManager(project);
            return;
        }
        if (NpmScanManager.isApplicable(project)) {
            scanManager = new NpmScanManager(project);
        }
    }

    public static ScanManager getScanManager(@NotNull Project project) {
        ScanManagerFactory scanManagerFactory = ServiceManager.getService(project, ScanManagerFactory.class);
        return scanManagerFactory.scanManager;
    }
}