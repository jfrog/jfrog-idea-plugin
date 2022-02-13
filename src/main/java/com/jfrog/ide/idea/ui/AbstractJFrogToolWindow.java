package com.jfrog.ide.idea.ui;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.ui.components.TitledPane;
import com.jfrog.ide.idea.ui.menus.filtermenu.IssueFilterMenu;
import com.jfrog.ide.idea.ui.menus.filtermenu.LicenseFilterMenu;
import com.jfrog.ide.idea.ui.menus.filtermenu.ScopeFilterMenu;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.*;

/**
 * @author yahavi
 */
public abstract class AbstractJFrogToolWindow extends SimpleToolWindowPanel implements Disposable {

    private final OnePixelSplitter rightVerticalSplit;
    final MessageBusConnection projectBusConnection;
    final MessageBusConnection appBusConnection;
    final ComponentsTree componentsTree;
    ComponentIssuesTable issuesTable;
    JScrollPane issuesDetailsScroll;
    JPanel moreInfoPanel;
    final Project project;

    /**
     * @param project   - Currently opened IntelliJ project
     * @param supported - True if the current opened project is supported by the plugin.
     *                  If not, show the "Unsupported project type" message.
     */
    public AbstractJFrogToolWindow(@NotNull Project project, boolean supported, ComponentsTree componentsTree) {
        super(true);
        this.projectBusConnection = project.getMessageBus().connect(this);
        this.appBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        this.componentsTree = componentsTree;
        this.project = project;
        JPanel toolbar = createActionToolbar();

        JComponent issuesPanel = createComponentsIssueDetailView();

        OnePixelSplitter leftVerticalSplit = new OnePixelSplitter(false, 0.5f);
        leftVerticalSplit.setFirstComponent(createComponentsTreeView());
        leftVerticalSplit.setSecondComponent(issuesPanel);

        rightVerticalSplit = new OnePixelSplitter(false, 0.6f);
        rightVerticalSplit.setVisible(false);
        rightVerticalSplit.setFirstComponent(leftVerticalSplit);
        rightVerticalSplit.setSecondComponent(createMoreInfoView(supported));

        setToolbar(toolbar);
        setContent(rightVerticalSplit);
        registerListeners();
    }

    /**
     * Create the action toolbar. That is the top toolbar.
     *
     * @return the action toolbar
     */
    abstract JPanel createActionToolbar();

    /**
     * Get "Dependencies (Issues #)" or "Components (Issues #)", according to the view.
     *
     * @return components tree title
     */
    abstract String getComponentsTreeTitle();

    /**
     * Create the more info view. That is the right panel.
     *
     * @param supported - True if the current opened project is supported by the plugin.
     *                  If now, show the "Unsupported project type" message.
     * @return the more info view
     */
    abstract JComponent createMoreInfoView(boolean supported);

    /**
     * Get issues to display in the issues table.
     *
     * @param selectedNodes - The selected nodes in the components tree
     * @return issues to display in the issues table
     */
    abstract Set<Issue> getIssuesToDisplay(List<DependencyTree> selectedNodes);

    /**
     * Create CI or local issues filter menu
     *
     * @return issues filter menu
     */
    abstract IssueFilterMenu createIssueFilterMenu();

    /**
     * Create CI or local licenses filter menu
     *
     * @return licenses filter menu
     */
    abstract LicenseFilterMenu createLicenseFilterMenu();

    /**
     * Create CI or local scopes filter menu
     *
     * @return scopes filter menu
     */
    abstract ScopeFilterMenu createScopeFilterMenu();

    JPanel createComponentsTreePanel(boolean addRefreshButton) {
        DefaultActionGroup actionGroup = new DefaultActionGroup(new CollapseAllAction(componentsTree), new ExpandAllAction(componentsTree));
        if (addRefreshButton) {
            actionGroup.addAction(ActionManager.getInstance().getAction("JFrog.RefreshLocal"), Constraints.FIRST);
        }

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

    JPanel createJFrogToolbar(ActionGroup actionGroup) {
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("JFrog toolbar", actionGroup, true);
        actionToolbar.setTargetComponent(this);
        JPanel toolbarPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarPanel.add(actionToolbar.getComponent());
        return toolbarPanel;
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

    /**
     * Update the issues table according to the user choice in the dependency tree.
     */
    public void updateIssuesTable() {
        List<DependencyTree> selectedNodes = getSelectedNodes();
        issuesTable.updateIssuesTable(getIssuesToDisplay(selectedNodes), selectedNodes);
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
     * Called after a change in the credentials.
     */
    public void onConfigurationChange() {
        rightVerticalSplit.setSecondComponent(createMoreInfoView(true));
        issuesTable.updateIssuesTable(new HashSet<>(), new LinkedList<>());
        issuesTable.addTableSelectionListener(moreInfoPanel);
    }

    /**
     * Register the issues tree listeners.
     */
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
        componentsTree.addRightClickListener();
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the project bus connection
        projectBusConnection.disconnect();
        // Disconnect and release resources from the application bus connection
        appBusConnection.disconnect();
    }
}
