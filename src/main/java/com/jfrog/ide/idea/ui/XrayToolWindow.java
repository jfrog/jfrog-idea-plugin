package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.idea.Events;
import com.jfrog.ide.idea.ui.issues.IssuesTab;
import com.jfrog.ide.idea.ui.issues.IssuesTree;
import com.jfrog.ide.idea.ui.licenses.LicensesTab;
import com.jfrog.ide.idea.ui.licenses.LicensesTree;
import org.jetbrains.annotations.NotNull;


/**
 * Created by yahavi
 */
public class XrayToolWindow {

    public static final float TITLE_FONT_SIZE = 15f;
    public static final int TITLE_LABEL_SIZE = (int) TITLE_FONT_SIZE + 10;
    public static final int SCROLL_BAR_SCROLLING_UNITS = 16;

    private LicensesTab licensesTab;
    private IssuesTab issuesTab;

    XrayToolWindow() {
        this.licensesTab = new LicensesTab();
        this.issuesTab = new IssuesTab();
    }

    void initToolWindow(@NotNull ToolWindow toolWindow, boolean supported) {
        ContentManager contentManager = toolWindow.getContentManager();
        addContent(contentManager, supported);
        registerListeners();
    }

    private void addContent(ContentManager contentManager, boolean supported) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content issuesContent = contentFactory.createContent(issuesTab.createIssuesViewTab(supported), "Issues", false);
        Content licenseContent = contentFactory.createContent(licensesTab.createLicenseInfoTab(supported), "Licenses Info", false);
        contentManager.addContent(issuesContent);
        contentManager.addContent(licenseContent);
    }

    private Events createOnConfigurationChangeHandler() {
        return () -> ApplicationManager.getApplication().invokeLater(() -> {
            issuesTab.onConfigurationChange();
            licensesTab.onConfigurationChange();
        });
    }

    private void applyFilters() {
        IssuesTree.getInstance().applyFiltersForAllProjects();
        LicensesTree.getInstance().applyFiltersForAllProjects();
    }

    private void registerListeners() {
        MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
        // Xray credentials were set listener
        busConnection.subscribe(Events.ON_CONFIGURATION_DETAILS_CHANGE, createOnConfigurationChangeHandler());

        // Idea framework change listener
        busConnection.subscribe(Events.ON_IDEA_FRAMEWORK_CHANGE, createOnConfigurationChangeHandler());

        busConnection.subscribe(Events.ON_SCAN_COMPONENTS_CHANGE, ()
                -> ApplicationManager.getApplication().invokeLater(this::applyFilters));

        busConnection.subscribe(Events.ON_SCAN_FILTER_CHANGE, () -> {
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(Events.ON_SCAN_COMPONENTS_CHANGE).update();
            messageBus.syncPublisher(Events.ON_SCAN_ISSUES_CHANGE).update();
        });

        // Issues tab listeners
        issuesTab.registerListeners(busConnection);

        // Licenses tab listeners
        licensesTab.registerListeners();
    }
}