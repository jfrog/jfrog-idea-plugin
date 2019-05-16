package com.jfrog.ide.idea.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class XrayToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        ScanManagersFactory scanManagersFactory = ScanManagersFactory.getInstance();
        try {
            scanManagersFactory.refreshScanManagers();
        } catch (IOException e) {
            // Ignore
        }
        boolean isSupported = CollectionUtils.isNotEmpty(ScanManagersFactory.getScanManagers());
        DumbService.getInstance(project).runWhenSmart(() -> {
            ServiceManager.getService(project, XrayToolWindow.class).initToolWindow(toolWindow, isSupported);
            scanManagersFactory.startScan(true, null);
        });
    }
}
