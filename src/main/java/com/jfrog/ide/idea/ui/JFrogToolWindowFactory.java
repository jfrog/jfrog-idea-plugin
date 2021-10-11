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
import static com.jfrog.ide.idea.utils.Utils.getScanLogicType;

/**
 * @author yahavi
 */
public class JFrogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        boolean localProjectSupported = isLocalProjectSupported(project);
        boolean buildsConfigured = isBuildsConfigured(project);
        DumbService.getInstance(project).runWhenSmart(() -> {
            ServiceManager.getService(project, JFrogToolWindow.class).initToolWindow(toolWindow, project, localProjectSupported, buildsConfigured);
            ScanManagersFactory.getInstance(project).startScan(true);
            CiManager.getInstance(project).asyncRefreshBuilds(true);
        });
    }

    private boolean isLocalProjectSupported(Project project) {
        ScanManagersFactory scanManagersFactory = ScanManagersFactory.getInstance(project);
        try {
            scanManagersFactory.refreshScanManagers(getScanLogicType());
        } catch (IOException e) {
            // Ignore
        }
        return CollectionUtils.isNotEmpty(ScanManagersFactory.getScanManagers(project));
    }

    private boolean isBuildsConfigured(Project project) {
        String buildsPattern = PropertiesComponent.getInstance(project).getValue(BUILDS_PATTERN_KEY);
        return StringUtils.isNotBlank(buildsPattern);
    }
}
