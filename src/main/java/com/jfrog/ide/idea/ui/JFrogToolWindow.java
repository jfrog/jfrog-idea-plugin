package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.idea.events.Events;
import com.jfrog.ide.idea.ui.issues.IssuesTab;
import com.jfrog.ide.idea.ui.issues.IssuesTree;
import com.jfrog.ide.idea.ui.licenses.LicensesTab;
import com.jfrog.ide.idea.ui.licenses.LicensesTree;
import org.jetbrains.annotations.NotNull;


/**
 * Created by yahavi
 */
public class JFrogToolWindow {

    public static final float TITLE_FONT_SIZE = 15f;
    public static final int TITLE_LABEL_SIZE = (int) TITLE_FONT_SIZE + 10;
    public static final int SCROLL_BAR_SCROLLING_UNITS = 16;

    private LicensesTab licensesTab;
    private IssuesTab issuesTab;

    JFrogToolWindow() {
        this.licensesTab = new LicensesTab();
        this.issuesTab = new IssuesTab();
    }

    void initToolWindow(@NotNull ToolWindow toolWindow, @NotNull Project mainProject, boolean supported) {
        ContentManager contentManager = toolWindow.getContentManager();
        addContent(contentManager, mainProject, supported);
        registerListeners(mainProject);
    }

    private void addContent(ContentManager contentManager, @NotNull Project project, boolean supported) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content issuesContent = contentFactory.createContent(issuesTab.createIssuesViewTab(project, supported), "Issues", false);
        Content licenseContent = contentFactory.createContent(licensesTab.createLicenseInfoTab(project, supported), "Licenses Info", false);
        contentManager.addContent(issuesContent);
        contentManager.addContent(licenseContent);
    }

    private void createOnConfigurationChangeHandler() {
        ApplicationManager.getApplication().invokeLater(() -> {
            issuesTab.onConfigurationChange();
            licensesTab.onConfigurationChange();
        });
    }

    private void registerListeners(@NotNull Project mainProject) {
        MessageBusConnection applicationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        // Xray credentials were set listener
        applicationBusConnection.subscribe(Events.ON_CONFIGURATION_DETAILS_CHANGE, this::createOnConfigurationChangeHandler);

        MessageBusConnection projectBusConnection = mainProject.getMessageBus().connect();
        projectBusConnection.subscribe(Events.ON_SCAN_FILTER_ISSUES_CHANGE, () -> ApplicationManager.getApplication().invokeLater(() -> {
            IssuesTree.getInstance(mainProject).applyFiltersForAllProjects();
            issuesTab.updateIssuesTable();
        }));

        projectBusConnection.subscribe(Events.ON_SCAN_FILTER_LICENSES_CHANGE, () -> ApplicationManager.getApplication().invokeLater(() ->
                LicensesTree.getInstance(mainProject).applyFiltersForAllProjects()));

        // Issues tab listeners
        issuesTab.registerListeners();

        // Licenses tab listeners
        licensesTab.registerListeners();
    }
}