package com.jfrog.ide.idea.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.ci.BuildGeneralInfo;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.events.BuildEvents;
import com.jfrog.ide.idea.ui.components.LinkButton;
import com.jfrog.ide.idea.ui.components.TitledPane;
import com.jfrog.ide.idea.ui.menus.builds.BuildsMenu;
import com.jfrog.ide.idea.ui.menus.filtermanager.CiFilterManager;
import com.jfrog.ide.idea.ui.menus.filtermenu.*;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.TITLE_FONT_SIZE;
import static com.jfrog.ide.idea.ui.JFrogToolWindow.TITLE_LABEL_SIZE;
import static com.jfrog.ide.idea.ui.utils.ComponentUtils.*;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yahavi
 **/
public class JFrogCiToolWindow extends AbstractJFrogToolWindow {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
    private LinkButton linkButton;
    private JLabel buildStarted;
    private JLabel buildStatus;
    private LinkButton seeMore;
    private JLabel branch;
    private JLabel commit;

    public JFrogCiToolWindow(@NotNull Project project, boolean buildsConfigured) {
        super(project, buildsConfigured, CiComponentsTree.getInstance(project));
    }

    @Override
    String getComponentsTreeTitle() {
        return " Build Components (Issues #)";
    }

    @Override
    IssueFilterMenu createIssueFilterMenu() {
        return new CiIssueFilterMenu(project);
    }

    @Override
    LicenseFilterMenu createLicenseFilterMenu() {
        return new CiLicenseFilterMenu(project);
    }

    @Override
    ScopeFilterMenu createScopeFilterMenu() {
        return new CiScopeFilterMenu(project);
    }

    @SuppressWarnings("DialogTitleCapitalization")
    @Override
    JComponent createMoreInfoView(boolean supported) {
        if (!GlobalSettings.getInstance().areArtifactoryCredentialsSet()) {
            return ComponentUtils.createNoCredentialsView();
        }
        JLabel title = new JBLabel(" More Info");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));

        moreInfoPanel = new JBPanel<>(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        moreInfoPanel.add(supported ? createDisabledTextLabel(SELECT_COMPONENT_TEXT) : createNoBuildsView(), BorderLayout.CENTER);
        issuesDetailsScroll = ScrollPaneFactory.createScrollPane(moreInfoPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, issuesDetailsScroll);
    }

    @Override
    public JPanel createActionToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup(ActionManager.getInstance().getAction("JFrog.RefreshBuilds"));
        JPanel toolbarPanel = createJFrogToolbar(actionGroup);

        // Add builds selector
        BuildsMenu buildsMenu = new BuildsMenu(project);
        ((CiComponentsTree) componentsTree).setBuildsMenu(buildsMenu);
        toolbarPanel.add(buildsMenu.getBuildButton());

        // Create parent toolbar containing the builds and the component tree toolbars
        JPanel parentToolbarPanel = new JBPanel<>(new GridLayout(2, 0));
        toolbarPanel.add(createBuildStatusPanel());
        parentToolbarPanel.add(ScrollPaneFactory.createScrollPane(toolbarPanel));
        parentToolbarPanel.add(createComponentsTreePanel(false));

        return parentToolbarPanel;
    }

    @Override
    public Set<Issue> getIssuesToDisplay(List<DependencyTree> selectedNodes) {
        return CiFilterManager.getInstance(project).getFilteredScanIssues(selectedNodes);
    }

    @Override
    public void registerListeners() {
        super.registerListeners();
        projectBusConnection.subscribe(ApplicationEvents.ON_CI_FILTER_CHANGE, () -> ApplicationManager.getApplication().invokeLater(() -> {
            CiComponentsTree.getInstance(project).applyFiltersForAllProjects();
            updateIssuesTable();
        }));
        projectBusConnection.subscribe(ApplicationEvents.ON_SCAN_CI_STARTED, () -> ApplicationManager.getApplication().invokeLater(this::resetViews));
        projectBusConnection.subscribe(BuildEvents.ON_SELECTED_BUILD, this::setBuildDetails);
        projectBusConnection.subscribe(ApplicationEvents.ON_BUILDS_CONFIGURATION_CHANGE, () -> ApplicationManager.getApplication().invokeLater(this::onConfigurationChange));
    }

    /**
     * Create the top panel with the build information.
     *
     * @return the build status panel
     */
    private JPanel createBuildStatusPanel() {
        JPanel buildStatusPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 20, 0));

        buildStatus = createAndAddLabelWithTooltip("Build status", buildStatusPanel);
        buildStarted = createAndAddLabelWithTooltip("Build timestamp", buildStatusPanel);
        branch = createAndAddLabelWithTooltip("Build branch", buildStatusPanel);
        commit = createAndAddLabelWithTooltip("The commit message that triggered the build", buildStatusPanel);
        linkButton = new LinkButton("Click to view the build log");
        seeMore = new LinkButton("See more in this view");
        buildStatusPanel.add(linkButton);
        buildStatusPanel.add(seeMore);

        return buildStatusPanel;
    }

    /**
     * Set the build details in the build details toolbar
     *
     * @param buildGeneralInfo - The build general info from Artifactory
     */
    private void setBuildDetails(BuildGeneralInfo buildGeneralInfo) {
        setBuildStarted(buildGeneralInfo);
        setBuildStatus(buildGeneralInfo);
        setSeeMore(buildGeneralInfo);
        setVcsInformation(buildGeneralInfo);
        setBuildLogLink(buildGeneralInfo);
    }

    private void setBuildStarted(BuildGeneralInfo buildGeneralInfo) {
        Date started = buildGeneralInfo != null ? buildGeneralInfo.getStarted() : null;
        setTextAndIcon(buildStarted, started != null ? DATE_FORMAT.format(started) : "", AllIcons.Actions.Profile);
    }

    private void setBuildStatus(BuildGeneralInfo buildGeneralInfo) {
        if (buildGeneralInfo == null) {
            setTextAndIcon(buildStatus, "", null);
            return;
        }
        switch (buildGeneralInfo.getStatus()) {
            case PASSED:
                setTextAndIcon(buildStatus, "Status: Success", AllIcons.RunConfigurations.TestPassed);
                return;
            case FAILED:
                setTextAndIcon(buildStatus, "Status: Failed", AllIcons.RunConfigurations.TestFailed);
                return;
            default:
                setTextAndIcon(buildStatus, "Status: Unknown", AllIcons.RunConfigurations.TestUnknown);
        }
    }

    private void setSeeMore(BuildGeneralInfo buildGeneralInfo) {
        Vcs vcs = buildGeneralInfo != null ? buildGeneralInfo.getVcs() : null;
        if (vcs == null || buildGeneralInfo.getStatus() == null ||
                isAnyBlank(vcs.getBranch(), vcs.getMessage(), buildGeneralInfo.getPath())) {
            seeMore.init(project, "See more in this view", "https://www.jfrog.com/confluence/display/JFROG/JFrog+IntelliJ+IDEA+Plugin");
        } else {
            seeMore.init(project, "", "");
        }
    }

    private void setVcsInformation(BuildGeneralInfo buildGeneralInfo) {
        Vcs vcs = buildGeneralInfo != null ? buildGeneralInfo.getVcs() : null;
        if (vcs == null) {
            setTextAndIcon(branch, "", null);
            setTextAndIcon(commit, "", null);
            return;
        }
        setTextAndIcon(branch, vcs.getBranch(), AllIcons.Vcs.Branch);
        setTextAndIcon(commit, vcs.getMessage(), AllIcons.Vcs.CommitNode);
    }

    private void setBuildLogLink(BuildGeneralInfo buildGeneralInfo) {
        String link = buildGeneralInfo != null ? buildGeneralInfo.getPath() : null;
        linkButton.init(project, "Build Log", link);
    }

    private JLabel createAndAddLabelWithTooltip(String tooltip, JPanel buildStatusPanel) {
        JLabel jLabel = new JBLabel();
        jLabel.setToolTipText(tooltip);
        buildStatusPanel.add(jLabel);
        return jLabel;
    }

    private void setTextAndIcon(JLabel label, String message, Icon icon) {
        if (isBlank(message)) {
            label.setText("");
            label.setIcon(null);
            return;
        }
        label.setText(message);
        label.setIcon(icon);
    }
}
