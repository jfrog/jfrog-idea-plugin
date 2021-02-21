package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.ui.components.TitledPane;
import com.jfrog.ide.idea.ui.filters.filtermanager.LocalFilterManager;
import com.jfrog.ide.idea.ui.filters.filtermenu.*;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.TITLE_FONT_SIZE;
import static com.jfrog.ide.idea.ui.JFrogToolWindow.TITLE_LABEL_SIZE;

/**
 * @author yahavi
 */
public class JFrogLocalToolWindow extends AbstractJFrogToolWindow {

    /**
     * @param mainProject - Currently opened IntelliJ project
     * @param supported   - True if the current opened project is supported by the plugin.
     *                    If not, show the "Unsupported project type" message.
     */
    public JFrogLocalToolWindow(@NotNull Project mainProject, boolean supported) {
        super(mainProject, supported, LocalComponentsTree.getInstance(mainProject));
    }

    @Override
    IssueFilterMenu createIssueFilterMenu() {
        return new LocalIssueFilterMenu(mainProject);
    }

    @Override
    LicenseFilterMenu createLicenseFilterMenu() {
        return new LocalLicenseFilterMenu(mainProject);
    }

    @Override
    ScopeFilterMenu createScopeFilterMenu() {
        return new LocalScopeFilterMenu(mainProject);
    }

    @Override
    public JPanel createActionToolbar() {
        return createComponentsTreePanel(true);
    }

    @Override
    JComponent createComponentsDetailsView(boolean supported) {
        if (!GlobalSettings.getInstance().areXrayCredentialsSet()) {
            return ComponentUtils.createNoCredentialsView();
        }
        JLabel title = new JBLabel(" Component Details");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));

        issuesDetailsPanel = new JBPanel<>(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        String panelText = supported ? ComponentUtils.SELECT_COMPONENT_TEXT : ComponentUtils.UNSUPPORTED_TEXT;
        issuesDetailsPanel.add(ComponentUtils.createDisabledTextLabel(panelText), BorderLayout.CENTER);
        issuesDetailsScroll = ScrollPaneFactory.createScrollPane(issuesDetailsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, issuesDetailsScroll);
    }

    @Override
    public Set<Issue> getIssuesToDisplay(List<DependencyTree> selectedNodes) {
        return LocalFilterManager.getInstance(mainProject).getFilteredScanIssues(selectedNodes);
    }

    /**
     * Register the issues tree listeners.
     */
    public void registerListeners() {
        super.registerListeners();
        MessageBusConnection projectBusConnection = mainProject.getMessageBus().connect();
        projectBusConnection.subscribe(ApplicationEvents.ON_SCAN_FILTER_CHANGE, () -> ApplicationManager.getApplication().invokeLater(() -> {
            LocalComponentsTree.getInstance(mainProject).applyFiltersForAllProjects();
            updateIssuesTable();
        }));
    }
}
