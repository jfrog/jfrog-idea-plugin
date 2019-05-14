package com.jfrog.ide.idea.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.idea.Events;
import com.jfrog.ide.idea.ui.issues.IssuesTab;
import com.jfrog.ide.idea.ui.licenses.LicensesTab;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.scan.ScanManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.util.Set;


/**
 * Created by romang on 3/7/17.
 */
public class XrayToolWindow {

    public static final float TITLE_FONT_SIZE = 15f;
    public static final int TITLE_LABEL_SIZE = (int) TITLE_FONT_SIZE + 10;
    public static final int SCROLL_BAR_SCROLLING_UNITS = 16;
    private final Project project;

    private LicensesTab licensesTab;
    private IssuesTab issuesTab;

    XrayToolWindow(@NotNull Project project) {
        this.project = project;
        this.licensesTab = new LicensesTab(project);
        this.issuesTab = new IssuesTab(project);
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

    private void registerListeners() {
        MessageBusConnection busConnection = project.getMessageBus().connect(project);
        // Xray credentials were set listener
        busConnection.subscribe(Events.ON_CONFIGURATION_DETAILS_CHANGE, createOnConfigurationChangeHandler());

        // Idea framework change listener
        busConnection.subscribe(Events.ON_IDEA_FRAMEWORK_CHANGE, createOnConfigurationChangeHandler());

        // Issues tab listeners
        issuesTab.registerListeners(busConnection);

        // Licenses tab listeners
        licensesTab.registerListeners();
    }

}