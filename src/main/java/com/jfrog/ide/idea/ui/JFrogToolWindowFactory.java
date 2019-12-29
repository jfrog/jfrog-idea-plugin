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

/**
 * @author yahavi
 */
public class JFrogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project mainProject, @NotNull final ToolWindow toolWindow) {
        ScanManagersFactory scanManagersFactory = ScanManagersFactory.getInstance(mainProject);
        try {
            scanManagersFactory.refreshScanManagers();
        } catch (IOException e) {
            // Ignore
        }
        boolean isSupported = CollectionUtils.isNotEmpty(ScanManagersFactory.getScanManagers(mainProject));
        DumbService.getInstance(mainProject).runWhenSmart(() -> {
            ServiceManager.getService(mainProject, JFrogToolWindow.class).initToolWindow(toolWindow, mainProject, isSupported);
            scanManagersFactory.startScan(true, null);
        });
    }
}
