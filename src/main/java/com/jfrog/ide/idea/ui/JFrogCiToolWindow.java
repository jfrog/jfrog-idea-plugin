package com.jfrog.ide.idea.ui;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.ci.BuildGeneralInfo;
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
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
import javax.swing.tree.TreePath;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.*;
import static com.jfrog.ide.idea.ui.utils.ComponentUtils.*;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yahavi
 **/
public class JFrogCiToolWindow extends AbstractJFrogToolWindow {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
    private static final String SELECT_COMPONENT_TEXT = "Select component or issue for more details.";
    private LinkButton linkButton;
    private JLabel buildStarted;
    private JLabel buildStatus;
    private LinkButton seeMore;
    private JLabel branch;
    private JLabel commit;
    private final OnePixelSplitter rightVerticalSplit;
    final CiComponentsTree componentsTree;
    ComponentIssuesTable issuesTable;
    JScrollPane issuesDetailsScroll;
    JPanel moreInfoPanel;

    public JFrogCiToolWindow(@NotNull Project project, boolean buildsConfigured) {
        super(project);
        this.componentsTree = CiComponentsTree.getInstance(project);
        JPanel toolbar = createActionToolbar();

        JComponent issuesPanel = createComponentsIssueDetailView();

        OnePixelSplitter leftVerticalSplit = new OnePixelSplitter(false, 0.5f);
        leftVerticalSplit.setFirstComponent(createComponentsTreeView());
        leftVerticalSplit.setSecondComponent(issuesPanel);

        rightVerticalSplit = new OnePixelSplitter(false, 0.6f);
        rightVerticalSplit.setVisible(false);
        rightVerticalSplit.setFirstComponent(leftVerticalSplit);
        rightVerticalSplit.setSecondComponent(createMoreInfoView(buildsConfigured));

        setToolbar(toolbar);
        setContent(rightVerticalSplit);
        registerListeners();
    }

    private String getComponentsTreeTitle() {
        return " Build Components (Issues #)";
    }

    /**
     * Create CI issues filter menu
     *
     * @return issues filter menu
     */
    IssueFilterMenu createIssueFilterMenu() {
        return new CiIssueFilterMenu(project);
    }

    /**
     * Create CI licenses filter menu
     *
     * @return licenses filter menu
     */
    LicenseFilterMenu createLicenseFilterMenu() {
        return new CiLicenseFilterMenu(project);
    }

    /**
     * Create CI scopes filter menu
     *
     * @return scopes filter menu
     */
    ScopeFilterMenu createScopeFilterMenu() {
        return new CiScopeFilterMenu(project);
    }

    /**
     * Create the more info view. That is the right panel.
     *
     * @param supported - True if the current opened project is supported by the plugin.
     *                  If now, show the "Unsupported project type" message.
     * @return the more info view
     */
    @SuppressWarnings("DialogTitleCapitalization")
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
        parentToolbarPanel.add(createComponentsTreePanel());

        return parentToolbarPanel;
    }

    private JPanel createComponentsTreePanel() {
        DefaultActionGroup actionGroup = new DefaultActionGroup(new CollapseAllAction(componentsTree), new ExpandAllAction(componentsTree));

        JPanel toolbarPanel = createJFrogToolbar(actionGroup);
        // Add issues filter
        IssueFilterMenu issueFilterMenu = createIssueFilterMenu();
        componentsTree.addFilterMenu(issueFilterMenu);
        toolbarPanel.add(issueFilterMenu.getFilterButton());

        // Add licenses filter
        LicenseFilterMenu licenseFilterMenu = createLicenseFilterMenu();
        componentsTree.addFilterMenu(licenseFilterMenu);
        toolbarPanel.add(licenseFilterMenu.getFilterButton());

        // Add scopes filter
        ScopeFilterMenu scopeFilterMenu = createScopeFilterMenu();
        componentsTree.addFilterMenu(scopeFilterMenu);
        toolbarPanel.add(scopeFilterMenu.getFilterButton());

        return toolbarPanel;
    }

    /**
     * Get issues to display in the issues table.
     *
     * @param selectedNodes - The selected nodes in the components tree
     * @return issues to display in the issues table
     */
    private Set<Issue> getIssuesToDisplay(List<DependencyTree> selectedNodes) {
        return CiFilterManager.getInstance(project).getFilteredScanIssues(selectedNodes);
    }

    public void registerListeners() {
        // Xray credentials were set listener
        appBusConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, () ->
                ApplicationManager.getApplication().invokeLater(this::onConfigurationChange));

        // Component selection listener
        componentsTree.addTreeSelectionListener(e -> {
            updateIssuesTable();
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            ComponentIssueDetails.createIssuesDetailsView(moreInfoPanel, (DependencyTree) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            ApplicationManager.getApplication().invokeLater(() -> issuesDetailsScroll.getViewport().setViewPosition(new Point()));
        });

        issuesTable.addTableSelectionListener(moreInfoPanel);
        componentsTree.addOnProjectChangeListener(projectBusConnection);
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
            seeMore.init(project, "See more in this view", "https://github.com/jfrog/jfrog-idea-plugin#the-ci-view");
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

    /**
     * Called after a change in the credentials.
     */
    @Override
    public void onConfigurationChange() {
        rightVerticalSplit.setSecondComponent(createMoreInfoView(true));
        super.onConfigurationChange();
        issuesTable.addTableSelectionListener(moreInfoPanel);
    }

    /**
     * Create the components tree panel.
     *
     * @return the components tree panel
     */
    private JComponent createComponentsTreeView() {
        JPanel componentsTreePanel = new JBPanel<>(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        JLabel componentsTreeTitle = new JBLabel(getComponentsTreeTitle());
        componentsTreeTitle.setFont(componentsTreeTitle.getFont().deriveFont(TITLE_FONT_SIZE));
        componentsTreePanel.add(componentsTreeTitle, BorderLayout.LINE_START);
        JPanel treePanel = new JBPanel<>(new GridLayout()).withBackground(UIUtil.getTableBackground());
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(componentsTree, ComponentUtils::getPathSearchString, true);
        treePanel.add(treeSpeedSearch.getComponent(), BorderLayout.WEST);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treePanel);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_BAR_SCROLLING_UNITS);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, componentsTreePanel, treeScrollPane);
    }

    /**
     * Return the selected nodes in the dependency tree.
     *
     * @return the selected nodes in the dependency tree
     */
    List<DependencyTree> getSelectedNodes() {
        if (componentsTree.getModel() == null) {
            return Lists.newArrayList();
        }
        // If no node selected - Return the root
        if (componentsTree.getSelectionPaths() == null) {
            return Lists.newArrayList((DependencyTree) componentsTree.getModel().getRoot());
        }
        return Arrays.stream(componentsTree.getSelectionPaths())
                .map(TreePath::getLastPathComponent)
                .map(obj -> (DependencyTree) obj)
                .collect(Collectors.toList());
    }

    /**
     * Update the issues table according to the user choice in the dependency tree.
     */
    public void updateIssuesTable() {
        List<DependencyTree> selectedNodes = getSelectedNodes();
        issuesTable.updateIssuesTable(getIssuesToDisplay(selectedNodes), selectedNodes);
    }

    /**
     * Create the issues details panel. That is the bottom right issues table.
     *
     * @return the issues details panel
     */
    private JComponent createComponentsIssueDetailView() {
        issuesTable = new ComponentIssuesTable();
        JScrollPane tableScroll = ScrollPaneFactory.createScrollPane(issuesTable, SideBorder.ALL);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        JLabel title = new JBLabel(" Vulnerabilities");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, tableScroll);
    }

    @Override
    void resetViews() {
        if (componentsTree != null) {
            componentsTree.reset();
        }
        if (issuesTable != null) {
            issuesTable.reset();
        }
    }
}
