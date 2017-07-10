package org.jfrog.idea.xray;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.sun.istack.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jfrog.idea.xray.scan.MavenScanManager;
import org.jfrog.idea.xray.scan.ScanManager;

/**
 * Created by romang on 3/2/17.
 */
public class ScanManagerFactory {
    private ScanManager scanManager;

    public ScanManagerFactory(Project project) {
        // create the proper scan manager according to the project type.
        if (MavenProjectsManager.getInstance(project).hasProjects()) {
            scanManager = new MavenScanManager(project);
        }
    }

    public static ScanManager getScanManager(@NotNull Project project) {
        ScanManagerFactory scanManagerFactory = ServiceManager.getService(project, ScanManagerFactory.class);
        return scanManagerFactory.scanManager;
    }
}
