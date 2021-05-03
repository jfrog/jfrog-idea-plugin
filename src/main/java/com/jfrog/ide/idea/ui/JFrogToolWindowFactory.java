package com.jfrog.ide.idea.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.jfrog.ide.idea.ci.CiManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.jfrog.ide.idea.ui.configuration.JFrogProjectConfiguration.BUILDS_PATTERN_KEY;

/**
 * @author yahavi
 */
public class JFrogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project mainProject, @NotNull final ToolWindow toolWindow) {
        boolean localProjectSupported = isLocalProjectSupported(mainProject);
        boolean buildsConfigured = isBuildsConfigured(mainProject);
        DumbService.getInstance(mainProject).runWhenSmart(() -> {
            ServiceManager.getService(mainProject, JFrogToolWindow.class).initToolWindow(toolWindow, mainProject, localProjectSupported, buildsConfigured);
            ScanManagersFactory.getInstance(mainProject).startScan(true);
            CiManager.getInstance(mainProject).asyncRefreshBuilds();
        });
    }

    private boolean isLocalProjectSupported(Project mainProject) {
        ScanManagersFactory scanManagersFactory = ScanManagersFactory.getInstance(mainProject);
        try {
            scanManagersFactory.refreshScanManagers();
        } catch (IOException e) {
            // Ignore
        }
        return CollectionUtils.isNotEmpty(ScanManagersFactory.getScanManagers(mainProject));
    }

    private boolean isBuildsConfigured(Project mainProject) {
        String buildsPattern = PropertiesComponent.getInstance(mainProject).getValue(BUILDS_PATTERN_KEY);
        return StringUtils.isNotBlank(buildsPattern);
    }
}
