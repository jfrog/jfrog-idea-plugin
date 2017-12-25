package org.jfrog.idea.ui.xray;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.xray.ScanManagerFactory;
import org.jfrog.idea.xray.scan.GradleScanManager;
import org.jfrog.idea.xray.scan.MavenScanManager;
import org.jfrog.idea.xray.scan.NpmScanManager;
import org.jfrog.idea.xray.scan.ScanManager;

public class XrayToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        boolean supported = isSupported(project);
        DumbService.getInstance(project).runWhenSmart(() -> ServiceManager.getService(project, XrayToolWindow.class).initToolWindow(toolWindow, supported));

        ScanManager scanManager = ScanManagerFactory.getScanManager(project);
        if (supported && GlobalSettings.getInstance().isCredentialsSet()) {
            scanManager.asyncScanAndUpdateResults(true);
        }
    }

    private boolean isSupported(Project project) {
        return MavenScanManager.isApplicable(project) ||
                GradleScanManager.isApplicable(project) ||
                NpmScanManager.isApplicable(project);
    }
}
