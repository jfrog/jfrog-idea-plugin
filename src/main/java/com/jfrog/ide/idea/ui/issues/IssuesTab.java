package com.jfrog.ide.idea.ui.issues;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.Events;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.ui.components.FilterButton;
import com.jfrog.ide.idea.ui.components.TitledPane;
import com.jfrog.ide.idea.ui.filters.IssueFilterMenu;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.jfrog.ide.idea.ui.XrayToolWindow.*;

/**
 * @author yahavi
 */
public class IssuesTab {

    private Map<TreePath, JPanel> issuesCountPanels = Maps.newHashMap();
    private IssuesTree issuesTree = IssuesTree.getInstance();
    private OnePixelSplitter issuesRightHorizontalSplit;
    private ComponentIssuesTable issuesTable;
    private JScrollPane issuesDetailsScroll;
    private JPanel issuesDetailsPanel;
    private JComponent issuesPanel;

    public JPanel createIssuesViewTab(boolean supported) {
        ActionToolbar toolbar = ComponentUtils.createActionToolbar(issuesTree);
        IssueFilterMenu issueFilterMenu = new IssueFilterMenu();
        JPanel filterButton = new FilterButton(issueFilterMenu, "Severity", "Select severities to show");
        SimpleToolWindowPanel filterPanel = new SimpleToolWindowPanel(false);
        filterPanel.setToolbar(toolbar.getComponent());
        filterPanel.setContent(filterButton);

        issuesPanel = createComponentsIssueDetailView();
        issuesRightHorizontalSplit = new OnePixelSplitter(true, 0.55f);
        issuesRightHorizontalSplit.setFirstComponent(createComponentsDetailsView(supported));
        issuesRightHorizontalSplit.setSecondComponent(issuesPanel);

        OnePixelSplitter centralVerticalSplit = new OnePixelSplitter(false, 0.20f);
        centralVerticalSplit.setFirstComponent(createIssuesComponentsTreeView());
        centralVerticalSplit.setSecondComponent(issuesRightHorizontalSplit);
        OnePixelSplitter issuesViewTab = new OnePixelSplitter(true, 0f);
        issuesViewTab.setResizeEnabled(false);
        issuesViewTab.setFirstComponent(filterPanel);
        issuesViewTab.setSecondComponent(centralVerticalSplit);
        return issuesViewTab;
    }

    private JComponent createIssuesComponentsTreeView() {
        JLabel issuesCount = new JBLabel("Issues (0) ");

        JPanel componentsTreePanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        JLabel componentsTreeTitle = new JBLabel(" Components Tree");
        componentsTreeTitle.setFont(componentsTreeTitle.getFont().deriveFont(TITLE_FONT_SIZE));
        componentsTreePanel.add(componentsTreeTitle, BorderLayout.LINE_START);
        componentsTreePanel.add(issuesCount, BorderLayout.LINE_END);

        JPanel issuesCountPanel = new JBPanel().withBackground(UIUtil.getTableBackground());
        issuesCountPanel.setLayout(new BoxLayout(issuesCountPanel, BoxLayout.Y_AXIS));
        issuesTree.createExpansionListener(issuesCountPanel, issuesCountPanels);
        issuesTree.setIssuesCountLabel(issuesCount);

        JBPanel treePanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(issuesTree, ComponentUtils::getPathSearchString, true);
        treePanel.add(treeSpeedSearch.getComponent(), BorderLayout.WEST);
        treePanel.add(issuesCountPanel, BorderLayout.CENTER);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treePanel);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_BAR_SCROLLING_UNITS);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, componentsTreePanel, treeScrollPane);
    }

    private JComponent createComponentsIssueDetailView() {
        issuesTable = new ComponentIssuesTable();
        JScrollPane tableScroll = ScrollPaneFactory.createScrollPane(issuesTable, SideBorder.ALL);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        JLabel title = new JBLabel(" Component Issues Details");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, tableScroll);
    }

    private JComponent createComponentsDetailsView(boolean supported) {
        if (!GlobalSettings.getInstance().areCredentialsSet()) {
            return ComponentUtils.createNoCredentialsView();
        }
        if (!supported) {
            return ComponentUtils.createUnsupportedView();
        }
        JLabel title = new JBLabel(" Component Details");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));

        issuesDetailsPanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        issuesDetailsPanel.add(ComponentUtils.createDisabledTextLabel("Select component or issue for more details"), BorderLayout.CENTER);
        issuesDetailsScroll = ScrollPaneFactory.createScrollPane(issuesDetailsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, issuesDetailsScroll);
    }

    private void updateIssuesTable() {
        List<DependenciesTree> selectedNodes = getSelectedNodes();
        Set<Issue> issueSet = ScanManagersFactory.getScanManagers()
                .stream()
                .map(scanManager -> scanManager.getFilteredScanIssues(selectedNodes))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        issuesTable.updateIssuesTable(issueSet);
    }

    private List<DependenciesTree> getSelectedNodes() {
        // If no node selected - Return the root
        if (issuesTree.getSelectionPaths() == null) {
            return Lists.newArrayList((DependenciesTree) issuesTree.getModel().getRoot());
        }
        return Arrays.stream(issuesTree.getSelectionPaths())
                .map(TreePath::getLastPathComponent)
                .map(obj -> (DependenciesTree) obj)
                .collect(Collectors.toList());
    }

    public void onConfigurationChange() {
        issuesRightHorizontalSplit.setFirstComponent(createComponentsDetailsView(true));
        issuesPanel.validate();
        issuesPanel.repaint();
    }

    public void registerListeners(MessageBusConnection busConnection) {
        issuesTree.addTreeExpansionListener();

        // Issues component selection listener
        issuesTree.addTreeSelectionListener(e -> {
            updateIssuesTable();
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            // Color the issues count panel
            for (TreePath path : e.getPaths()) {
                JPanel issueCountPanel = issuesCountPanels.get(path);
                if (issueCountPanel != null) {
                    issueCountPanel.setBackground(e.isAddedPath(path) ? UIUtil.getTreeSelectionBackground() : UIUtil.getTableBackground());
                }
            }
            ComponentIssueDetails.createIssuesDetailsView(issuesDetailsPanel, (DependenciesTree) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            ApplicationManager.getApplication().invokeLater(() -> issuesDetailsScroll.getViewport().setViewPosition(new Point()));
        });

        // Issues table listener
        busConnection.subscribe(Events.ON_SCAN_ISSUES_CHANGE, () -> ApplicationManager.getApplication().invokeLater(this::updateIssuesTable));

    }
}
