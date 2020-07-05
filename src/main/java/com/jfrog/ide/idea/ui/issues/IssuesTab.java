package com.jfrog.ide.idea.ui.issues;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.ui.components.FilterButton;
import com.jfrog.ide.idea.ui.components.TitledPane;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.filters.IssueFilterMenu;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.*;

/**
 * @author yahavi
 */
public class IssuesTab {

    private OnePixelSplitter issuesRightHorizontalSplit;
    private ComponentIssuesTable issuesTable;
    private JScrollPane issuesDetailsScroll;
    private JPanel issuesDetailsPanel;
    private JComponent issuesPanel;
    private IssuesTree issuesTree;
    private Project mainProject;

    /**
     * @param mainProject - Currently opened IntelliJ project
     * @param supported   - True if the current opened project is supported by the plugin.
     *                    If not, show the "Unsupported project type" message.
     * @return the issues view panel
     */
    public JPanel createIssuesViewTab(@NotNull Project mainProject, boolean supported) {
        this.mainProject = mainProject;
        this.issuesTree = IssuesTree.getInstance(mainProject);
        IssueFilterMenu issueFilterMenu = new IssueFilterMenu(mainProject);
        JPanel issuesFilterButton = new FilterButton(issueFilterMenu, "Severity", "Select severities to show");
        JPanel toolbar = ComponentUtils.createActionToolbar("Severities toolbar", issuesFilterButton, issuesTree);

        issuesPanel = createComponentsIssueDetailView();
        issuesRightHorizontalSplit = new OnePixelSplitter(true, 0.55f);
        issuesRightHorizontalSplit.setFirstComponent(createComponentsDetailsView(supported));
        issuesRightHorizontalSplit.setSecondComponent(issuesPanel);

        OnePixelSplitter centralVerticalSplit = new OnePixelSplitter(false, 0.20f);
        centralVerticalSplit.setFirstComponent(createIssuesComponentsTreeView());
        centralVerticalSplit.setSecondComponent(issuesRightHorizontalSplit);

        SimpleToolWindowPanel issuesViewTab = new SimpleToolWindowPanel(true);
        issuesViewTab.setToolbar(toolbar);
        issuesViewTab.setContent(centralVerticalSplit);
        return issuesViewTab;
    }

    /**
     * Create the issues tree panel.
     *
     * @return the issues tree panel
     */
    private JComponent createIssuesComponentsTreeView() {
        JPanel componentsTreePanel = new JBPanel<>(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        JLabel componentsTreeTitle = new JBLabel(" Component (Issues #)");
        componentsTreeTitle.setFont(componentsTreeTitle.getFont().deriveFont(TITLE_FONT_SIZE));
        componentsTreePanel.add(componentsTreeTitle, BorderLayout.LINE_START);
        JPanel treePanel = new JBPanel<>(new GridLayout()).withBackground(UIUtil.getTableBackground());
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(issuesTree, ComponentUtils::getPathSearchString, true);
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
        JLabel title = new JBLabel(" Component Issues Details");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, tableScroll);
    }

    /**
     * Create the component details view. That is the top right details panel.
     *
     * @param supported - True if the current opened project is supported by the plugin.
     *                  If now, show the "Unsupported project type" message.
     * @return the component details view
     */
    private JComponent createComponentsDetailsView(boolean supported) {
        if (!GlobalSettings.getInstance().areCredentialsSet()) {
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

    /**
     * Update the issues table according to the user choice in the dependencies tree.
     */
    public void updateIssuesTable() {
        List<DependenciesTree> selectedNodes = getSelectedNodes();
        Set<Issue> issueSet = ScanManagersFactory.getScanManagers(mainProject)
                .stream()
                .map(scanManager -> scanManager.getFilteredScanIssues(FilterManagerService.getInstance(mainProject), selectedNodes))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Set<String> selectedNodeNames = selectedNodes.stream().map(DefaultMutableTreeNode::toString).collect(Collectors.toSet());
        issuesTable.updateIssuesTable(issueSet, selectedNodeNames);
    }

    /**
     * Return the selected nodes in the dependencies tree.
     *
     * @return the selected nodes in the dependencies tree
     */
    private List<DependenciesTree> getSelectedNodes() {
        if (issuesTree.getModel() == null) {
            return Lists.newArrayList();
        }
        // If no node selected - Return the root
        if (issuesTree.getSelectionPaths() == null) {
            return Lists.newArrayList((DependenciesTree) issuesTree.getModel().getRoot());
        }
        return Arrays.stream(issuesTree.getSelectionPaths())
                .map(TreePath::getLastPathComponent)
                .map(obj -> (DependenciesTree) obj)
                .collect(Collectors.toList());
    }

    /**
     * Called after a change in the credentials.
     */
    public void onConfigurationChange() {
        issuesRightHorizontalSplit.setFirstComponent(createComponentsDetailsView(true));
        issuesPanel.validate();
        issuesPanel.repaint();
    }

    /**
     * Register the issues tree listeners.
     */
    public void registerListeners() {
        // Issues component selection listener
        issuesTree.addTreeSelectionListener(e -> {
            updateIssuesTable();
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            ComponentIssueDetails.createIssuesDetailsView(issuesDetailsPanel, (DependenciesTree) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            ApplicationManager.getApplication().invokeLater(() -> issuesDetailsScroll.getViewport().setViewPosition(new Point()));
        });

        issuesTree.addOnProjectChangeListener(mainProject.getMessageBus().connect());
        issuesTree.addRightClickListener();
    }
}
