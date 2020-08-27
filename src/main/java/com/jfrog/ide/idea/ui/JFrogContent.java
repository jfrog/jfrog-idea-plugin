package com.jfrog.ide.idea.ui;

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
import com.jfrog.ide.idea.ui.components.TitledPane;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
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
public class JFrogContent extends SimpleToolWindowPanel {

    private final OnePixelSplitter rightHorizontalSplit;
    private final ComponentsTree componentsTree;
    private ComponentIssuesTable issuesTable;
    private JScrollPane issuesDetailsScroll;
    private final JComponent issuesPanel;
    private final Project mainProject;
    private JPanel issuesDetailsPanel;

    /**
     * @param mainProject - Currently opened IntelliJ project
     * @param supported   - True if the current opened project is supported by the plugin.
     *                    If not, show the "Unsupported project type" message.
     */
    public JFrogContent(@NotNull Project mainProject, boolean supported) {
        super(true);
        this.mainProject = mainProject;
        this.componentsTree = ComponentsTree.getInstance(mainProject);
        JPanel toolbar = ComponentUtils.createActionToolbar("JFrog toolbar", mainProject, componentsTree);

        issuesPanel = createComponentsIssueDetailView();
        rightHorizontalSplit = new OnePixelSplitter(true, 0.55f);
        rightHorizontalSplit.setFirstComponent(createComponentsDetailsView(supported));
        rightHorizontalSplit.setSecondComponent(issuesPanel);

        OnePixelSplitter centralVerticalSplit = new OnePixelSplitter(false, 0.20f);
        centralVerticalSplit.setFirstComponent(createComponentsTreeView());
        centralVerticalSplit.setSecondComponent(rightHorizontalSplit);

        setToolbar(toolbar);
        setContent(centralVerticalSplit);
    }

    /**
     * Create the components tree panel.
     *
     * @return the components tree panel
     */
    private JComponent createComponentsTreeView() {
        JPanel componentsTreePanel = new JBPanel<>(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        JLabel componentsTreeTitle = new JBLabel(" Component (Issues #)");
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
        if (componentsTree.getModel() == null) {
            return Lists.newArrayList();
        }
        // If no node selected - Return the root
        if (componentsTree.getSelectionPaths() == null) {
            return Lists.newArrayList((DependenciesTree) componentsTree.getModel().getRoot());
        }
        return Arrays.stream(componentsTree.getSelectionPaths())
                .map(TreePath::getLastPathComponent)
                .map(obj -> (DependenciesTree) obj)
                .collect(Collectors.toList());
    }

    /**
     * Called after a change in the credentials.
     */
    public void onConfigurationChange() {
        rightHorizontalSplit.setFirstComponent(createComponentsDetailsView(true));
        issuesPanel.validate();
        issuesPanel.repaint();
    }

    /**
     * Register the issues tree listeners.
     */
    public void registerListeners() {
        // Component selection listener
        componentsTree.addTreeSelectionListener(e -> {
            updateIssuesTable();
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            ComponentIssueDetails.createIssuesDetailsView(issuesDetailsPanel, (DependenciesTree) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            ApplicationManager.getApplication().invokeLater(() -> issuesDetailsScroll.getViewport().setViewPosition(new Point()));
        });

        componentsTree.addOnProjectChangeListener(mainProject.getMessageBus().connect());
        componentsTree.addRightClickListener();
    }
}
