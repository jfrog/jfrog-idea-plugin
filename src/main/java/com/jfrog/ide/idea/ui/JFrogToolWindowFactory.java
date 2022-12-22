package com.jfrog.ide.idea.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.jfrog.ide.idea.ci.CiManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.jfrog.ide.idea.ui.configuration.JFrogProjectConfiguration.BUILDS_PATTERN_KEY;

/**
 * @author yahavi
 */
public class JFrogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        boolean buildsConfigured = isBuildsConfigured(project);
        DumbService.getInstance(project).runWhenSmart(() -> {
            project.getService(JFrogToolWindow.class).initToolWindow(toolWindow, project, buildsConfigured);
            ScanManagersFactory.getInstance(project).startScan();
            CiManager.getInstance(project).asyncRefreshBuilds();
        });
    }

    private boolean isBuildsConfigured(Project project) {
        String buildsPattern = PropertiesComponent.getInstance(project).getValue(BUILDS_PATTERN_KEY);
        return StringUtils.isNotBlank(buildsPattern);
    }
}
