package com.jfrog.ide.idea.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.jfrog.ide.idea.ci.CiManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.scan.ScanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.jfrog.ide.idea.ui.configuration.JFrogProjectConfiguration.BUILDS_PATTERN_KEY;

/**
 * @author yahavi
 */
public class JFrogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        boolean localProjectSupported = isLocalProjectSupported(project);
        boolean buildsConfigured = isBuildsConfigured(project);
        DumbService.getInstance(project).runWhenSmart(() -> {
            project.getService(JFrogToolWindow.class).initToolWindow(toolWindow, project, localProjectSupported, buildsConfigured);
            ScanManagersFactory.getInstance(project).startScan(true, false);
            CiManager.getInstance(project).asyncRefreshBuilds(true);
        });
    }

    private boolean isLocalProjectSupported(Project project) {
        try {
            return ScanUtils.isLocalProjectSupported(project);
        } catch (IOException ignored) {
            // If an IO exception occurred, the PackageFileFinder couldn't search for local projects.
            // In that case, we assume the local project is not supported.
            return false;
        }
    }

    private boolean isBuildsConfigured(Project project) {
        String buildsPattern = PropertiesComponent.getInstance(project).getValue(BUILDS_PATTERN_KEY);
        return StringUtils.isNotBlank(buildsPattern);
    }
}
