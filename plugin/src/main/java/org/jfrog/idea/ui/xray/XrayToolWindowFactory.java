package org.jfrog.idea.ui.xray;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.xray.ScanManagerFactory;
import org.jfrog.idea.xray.scan.ScanManager;

public class XrayToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        ScanManager scanManager = ScanManagerFactory.getScanManager(project);
        boolean isSupported = scanManager != null;
        DumbService.getInstance(project).runWhenSmart(() -> ServiceManager.getService(project, XrayToolWindow.class).initToolWindow(toolWindow, isSupported));
        if (isSupported && GlobalSettings.getInstance().isCredentialsSet()) {
            scanManager.asyncScanAndUpdateResults(true);
        }
    }
}
