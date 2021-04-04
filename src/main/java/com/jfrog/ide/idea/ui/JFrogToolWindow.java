package com.jfrog.ide.idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;


/**
 * Created by yahavi
 */
public class JFrogToolWindow {

    public static final float TITLE_FONT_SIZE = 15f;
    public static final int TITLE_LABEL_SIZE = (int) TITLE_FONT_SIZE + 10;
    public static final int SCROLL_BAR_SCROLLING_UNITS = 16;

    void initToolWindow(@NotNull ToolWindow toolWindow, @NotNull Project mainProject, boolean localProjectSupported, boolean buildsConfigured) {
        ContentManager contentManager = toolWindow.getContentManager();
        JFrogLocalToolWindow jfrogLocalContent = new JFrogLocalToolWindow(mainProject, localProjectSupported);
        JFrogCiToolWindow jFrogCiContent = new JFrogCiToolWindow(mainProject, buildsConfigured);
        addContent(contentManager, jfrogLocalContent, jFrogCiContent);
    }

    private void addContent(ContentManager contentManager, JFrogLocalToolWindow jfrogLocalContent, JFrogCiToolWindow jfrogBuildsContent) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content localContent = contentFactory.createContent(jfrogLocalContent, "Local", false);
        contentManager.addContent(localContent);
        Content buildsContent = contentFactory.createContent(jfrogBuildsContent, "CI", false);
        contentManager.addContent(buildsContent);
    }
}