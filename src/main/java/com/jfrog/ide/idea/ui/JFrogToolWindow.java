package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;


/**
 * Created by yahavi
 */
public class JFrogToolWindow {

    public static final float TITLE_FONT_SIZE = 15f;
    public static final int TITLE_LABEL_SIZE = (int) TITLE_FONT_SIZE + 10;
    public static final int SCROLL_BAR_SCROLLING_UNITS = 16;

    void initToolWindow(@NotNull ToolWindow toolWindow, @NotNull Project mainProject, boolean supported) {
        ContentManager contentManager = toolWindow.getContentManager();
        JFrogContent content = new JFrogContent(mainProject, supported);
        addContent(contentManager, content);
        registerListeners(mainProject, content);
    }

    private void addContent(ContentManager contentManager, JFrogContent content) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content issuesContent = contentFactory.createContent(content, "", false);
        contentManager.addContent(issuesContent);
    }

    private void createOnConfigurationChangeHandler(JFrogContent JFrogContent) {
        ApplicationManager.getApplication().invokeLater(JFrogContent::onConfigurationChange);
    }

    private void registerListeners(@NotNull Project mainProject, JFrogContent content) {
        MessageBusConnection applicationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        // Xray credentials were set listener
        applicationBusConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, () -> createOnConfigurationChangeHandler(content));

        MessageBusConnection projectBusConnection = mainProject.getMessageBus().connect();
        projectBusConnection.subscribe(ApplicationEvents.ON_SCAN_FILTER_CHANGE, () -> ApplicationManager.getApplication().invokeLater(() -> {
            ComponentsTree.getInstance(mainProject).applyFiltersForAllProjects();
            content.updateIssuesTable();
        }));

        // Issues tab listeners
        content.registerListeners();
    }
}