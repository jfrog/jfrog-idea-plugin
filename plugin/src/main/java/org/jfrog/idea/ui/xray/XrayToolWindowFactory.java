package org.jfrog.idea.ui.xray;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.xray.ScanManagersFactory;
import org.jfrog.idea.xray.scan.ScanManager;

import java.util.List;

public class XrayToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        List<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(project);
        boolean isSupported = CollectionUtils.isNotEmpty(scanManagers);
        DumbService.getInstance(project).runWhenSmart(() ->
                ServiceManager.getService(project, XrayToolWindow.class).initToolWindow(toolWindow, isSupported));
        if (isSupported && GlobalSettings.getInstance().isCredentialsSet()) {
            scanManagers.forEach(scanManager -> scanManager.asyncScanAndUpdateResults(true));
        }
    }
}
