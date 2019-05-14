package com.jfrog.ide.idea.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class XrayToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers();
        boolean isSupported = CollectionUtils.isNotEmpty(scanManagers);
        DumbService.getInstance(project).runWhenSmart(() ->
                ServiceManager.getService(project, XrayToolWindow.class).initToolWindow(toolWindow, isSupported));
        if (isSupported && GlobalSettings.getInstance().isCredentialsSet()) {
            scanManagers.forEach(scanManager -> scanManager.asyncScanAndUpdateResults(true));
        }
    }
}
