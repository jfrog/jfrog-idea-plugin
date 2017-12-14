package org.jfrog.idea.ui.xray;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.Events;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.ui.components.FilterButton;
import org.jfrog.idea.ui.components.IssuesTable;
import org.jfrog.idea.ui.components.TitledPane;
import org.jfrog.idea.ui.configuration.XrayGlobalConfiguration;
import org.jfrog.idea.ui.utils.ComponentUtils;
import org.jfrog.idea.ui.xray.filters.IssueFilterMenu;
import org.jfrog.idea.ui.xray.filters.LicenseFilterMenu;
import org.jfrog.idea.ui.xray.models.IssuesTableModel;
import org.jfrog.idea.ui.xray.renderers.IssuesTreeCellRenderer;
import org.jfrog.idea.ui.xray.renderers.LicensesTreeCellRenderer;
import org.jfrog.idea.xray.ScanManagerFactory;
import org.jfrog.idea.xray.ScanTreeNode;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by romang on 3/7/17.
 */
public class XrayToolWindow implements Disposable {

    private static final float TITLE_FONT_SIZE = 15f;
    private static final int TITLE_LABEL_SIZE = (int) TITLE_FONT_SIZE + 10;
    private static final int SCROLL_BAR_SCROLLING_UNITS = 16;
    private final Project project;

    // Issues Tab
    private Tree issuesComponentsTree = new Tree(new ScanTreeNode(null));
    private JPanel issuesDetailsPanel;
    private JScrollPane issuesDetailsScroll;
    private OnePixelSplitter issuesRightHorizontalSplit;
    private JComponent issuesPanel;
    private JBTable issuesTable;
    private JLabel issuesCount;
    private JPanel issuesCountPanel;
    private Map<TreePath, JPanel> issuesCountPanels = Maps.newHashMap();

    // Licenses Tab
    private Tree licensesComponentsTree = new Tree(new ScanTreeNode(null));
    private JPanel licensesDetailsPanel;
    private JScrollPane licensesDetailsScroll;
    private LicenseFilterMenu licenseFilterMenu;
    private OnePixelSplitter licensesCentralVerticalSplit;

    XrayToolWindow(@NotNull Project project) {
        this.project = project;
    }

    void initToolWindow(@NotNull ToolWindow toolWindow, boolean supported) {
        ContentManager contentManager = toolWindow.getContentManager();
        if (!supported) {
            contentManager.addContent(createUnsupportedView());
            return;
        }

        addContent(contentManager);
        registerListeners();
    }

    private void addContent(ContentManager contentManager) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content issuesContent = contentFactory.createContent(createIssuesViewTab(), "Issues", false);
        Content licenseContent = contentFactory.createContent(createLicenseInfoTab(), "Licenses Info", false);
        issuesContent.setCloseable(false);
        licenseContent.setCloseable(false);
        populateTrees();
        contentManager.addContent(issuesContent);
        contentManager.addContent(licenseContent);
    }

    private void populateTrees() {
        licenseFilterMenu.setLicenses();
        TreeModel issuesTreeModel = new DefaultTreeModel(new ScanTreeNode("SeveritiesTree"));
        TreeModel licensesTreeModel = new DefaultTreeModel(new ScanTreeNode("LicensesTree"));
        ScanManagerFactory.getScanManager(project).filterAndSort(issuesTreeModel, licensesTreeModel);

        ScanTreeNode root = (ScanTreeNode) issuesTreeModel.getRoot();
        issuesCount.setText("Issues (" + root.getIssueCount() + ") ");

        issuesComponentsTree.setModel(issuesTreeModel);
        issuesComponentsTree.validate();
        issuesComponentsTree.repaint();
        licensesComponentsTree.setModel(licensesTreeModel);
        licensesComponentsTree.validate();
        licensesComponentsTree.repaint();

        setIssuesCountPanel();
    }

    private JPanel createIssuesViewTab() {
        ActionToolbar toolbar = ComponentUtils.createActionToolbar(issuesComponentsTree);
        IssueFilterMenu issueFilterMenu = new IssueFilterMenu(project);
        JPanel filterButton = new FilterButton(issueFilterMenu, "Severity", "Select severities to show");
        SimpleToolWindowPanel filterPanel = new SimpleToolWindowPanel(false);
        filterPanel.setToolbar(toolbar.getComponent());
        filterPanel.setContent(filterButton);

        issuesPanel = createComponentsIssueDetailView();
        issuesRightHorizontalSplit = new OnePixelSplitter(true, 0.55f);
        issuesRightHorizontalSplit.setFirstComponent(createComponentsDetailsView());
        issuesRightHorizontalSplit.setSecondComponent(issuesPanel);

        OnePixelSplitter centralVerticalSplit = new OnePixelSplitter(false, 0.33f);
        centralVerticalSplit.setFirstComponent(createIssuesComponentsTreeView());
        centralVerticalSplit.setSecondComponent(issuesRightHorizontalSplit);
        OnePixelSplitter issuesViewTab = new OnePixelSplitter(true, 0f);
        issuesViewTab.setResizeEnabled(false);
        issuesViewTab.setFirstComponent(filterPanel);
        issuesViewTab.setSecondComponent(centralVerticalSplit);
        return issuesViewTab;
    }

    private JPanel createLicenseInfoTab() {
        ActionToolbar toolbar = ComponentUtils.createActionToolbar(licensesComponentsTree);
        licenseFilterMenu = new LicenseFilterMenu(project);
        FilterButton licensesFilterButton = new FilterButton(licenseFilterMenu, "License", "Select licenses to show");
        licensesFilterButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        SimpleToolWindowPanel filterPanel = new SimpleToolWindowPanel(false);
        filterPanel.setToolbar(toolbar.getComponent());
        filterPanel.setContent(licensesFilterButton);

        JPanel licenseTab = new JBPanel(new BorderLayout());
        licensesCentralVerticalSplit = new OnePixelSplitter(false, 0.3f);
        licensesCentralVerticalSplit.setFirstComponent(createLicensesComponentsTreeView());
        licensesCentralVerticalSplit.setSecondComponent(createLicenseDetailsView());
        licenseTab.add(filterPanel, BorderLayout.NORTH);
        licenseTab.add(licensesCentralVerticalSplit, BorderLayout.CENTER);
        return licenseTab;
    }

    private Content createUnsupportedView() {
        JPanel panel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        panel.add(ComponentUtils.createDisabledTextLabel("Unsupported project type, currently only Maven and Gradle projects are supported."), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        return contentFactory.createContent(panel, "unsupported project type", false);
    }

    private void registerListeners() {
        MessageBusConnection busConnection = project.getMessageBus().connect(project);
        // Xray credentials were set listener
        busConnection.subscribe(Events.ON_CONFIGURATION_DETAILS_CHANGE, ()
                -> ApplicationManager.getApplication().invokeLater(() -> {
            issuesRightHorizontalSplit.setFirstComponent(createComponentsDetailsView());
            licensesCentralVerticalSplit.setSecondComponent(createLicenseDetailsView());
            issuesPanel.validate();
            issuesPanel.repaint();
        }));

        // Component tree change listener
        busConnection.subscribe(Events.ON_SCAN_COMPONENTS_CHANGE, ()
                -> ApplicationManager.getApplication().invokeLater(this::populateTrees));

        // Issues component selection listener
        issuesComponentsTree.addTreeSelectionListener(e -> {
            updateIssuesTable();
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            DetailsViewFactory.createIssuesDetailsView(issuesDetailsPanel, (ScanTreeNode) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            SwingUtilities.invokeLater(() -> issuesDetailsScroll.getViewport().setViewPosition(new Point()));
        });

        // License component selection listener
        licensesComponentsTree.addTreeSelectionListener(e -> {
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            DetailsViewFactory.createLicenseDetailsView(licensesDetailsPanel, (ScanTreeNode) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            SwingUtilities.invokeLater(() -> licensesDetailsScroll.getViewport().setViewPosition(new Point()));
        });

        // Issues update listener
        busConnection.subscribe(Events.ON_SCAN_ISSUES_CHANGE, () -> ApplicationManager.getApplication().invokeLater(this::updateIssuesTable));
    }

    private JComponent createComponentsDetailsView() {
        if (!GlobalSettings.getInstance().isCredentialsSet()) {
            return createNoCredentialsView();
        }
        JLabel title = new JBLabel(" Component Details");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));

        issuesDetailsPanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        issuesDetailsPanel.add(ComponentUtils.createDisabledTextLabel("Select component or issue for more details"), BorderLayout.CENTER);
        issuesDetailsScroll = ScrollPaneFactory.createScrollPane(issuesDetailsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, issuesDetailsScroll);
    }

    private JComponent createLicenseDetailsView() {
        if (!GlobalSettings.getInstance().isCredentialsSet()) {
            return createNoCredentialsView();
        }
        JLabel title = new JBLabel(" Details");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));

        licensesDetailsPanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        licensesDetailsPanel.add(ComponentUtils.createDisabledTextLabel("Select component or issue for more details"), BorderLayout.CENTER);
        licensesDetailsScroll = ScrollPaneFactory.createScrollPane(licensesDetailsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, licensesDetailsScroll);
    }

    private JComponent createIssuesComponentsTreeView() {
        issuesCount = new JBLabel("Issues (0) ");

        JPanel componentsTreePanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        JLabel componentsTreeTitle = new JBLabel(" Components Tree");
        componentsTreeTitle.setFont(componentsTreeTitle.getFont().deriveFont(TITLE_FONT_SIZE));
        componentsTreePanel.add(componentsTreeTitle, BorderLayout.LINE_START);
        componentsTreePanel.add(issuesCount, BorderLayout.LINE_END);

        issuesCountPanel = new JBPanel().withBackground(UIUtil.getTableBackground());
        issuesCountPanel.setLayout(new BoxLayout(issuesCountPanel, BoxLayout.Y_AXIS));
        issuesComponentsTree.setCellRenderer(new IssuesTreeCellRenderer());
        issuesComponentsTree.expandRow(0);
        issuesComponentsTree.setRootVisible(false);
        setTreeExpansionListener();
        setTreeSelectionListener();

        JBPanel treePanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(issuesComponentsTree);
        treePanel.add(treeSpeedSearch.getComponent(), BorderLayout.WEST);
        treePanel.add(issuesCountPanel, BorderLayout.CENTER);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treePanel);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_BAR_SCROLLING_UNITS);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, componentsTreePanel, treeScrollPane);
    }

    private void setTreeExpansionListener() {
        issuesComponentsTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                setIssuesCountPanel();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                setIssuesCountPanel();
            }
        });
    }

    private void setTreeSelectionListener() {
        issuesComponentsTree.addTreeSelectionListener(e -> {
            TreePath[] paths = e.getPaths();
            for (TreePath path : paths) {
                JPanel issueCountPanel = issuesCountPanels.get(path);
                if (e.isAddedPath(path)) {
                    issueCountPanel.setBackground(UIUtil.getTreeSelectionBackground());
                } else {
                    issueCountPanel.setBackground(UIUtil.getTableBackground());
                }
            }
        });
    }

    private void setIssuesCountPanel() {
        issuesCountPanel.removeAll();
        ScanTreeNode root = (ScanTreeNode) issuesComponentsTree.getModel().getRoot();
        setIssuesCountPanel(root, ComponentUtils.getTreePath(root));
    }

    private void setIssuesCountPanel(ScanTreeNode root, TreePath rootPath) {
        if (issuesComponentsTree.isExpanded(rootPath)) {
            root.getChildren().forEach(child -> {
                TreePath childTreePath = ComponentUtils.getTreePath(child);
                JPanel issueCountPanel = issuesCountPanels.get(childTreePath);
                if (issueCountPanel != null) {
                    ComponentUtils.setIssueCountPanel(child.getIssueCount(), issueCountPanel);
                } else {
                    issueCountPanel = ComponentUtils.createIssueCountLabel(child.getIssueCount(), issuesComponentsTree.getRowBounds(0).height);
                    issuesCountPanels.put(childTreePath, issueCountPanel);
                }
                issuesCountPanel.add(issueCountPanel);
                setIssuesCountPanel(child, childTreePath);
            });
        }
    }

    private JComponent createLicensesComponentsTreeView() {
        JPanel componentsTreePanel = new JBPanel(new BorderLayout());
        componentsTreePanel.setBackground(UIUtil.getTableBackground());
        JLabel componentsTreeTitle = new JBLabel(" Components Tree");
        componentsTreeTitle.setFont(componentsTreeTitle.getFont().deriveFont(TITLE_FONT_SIZE));
        componentsTreePanel.add(componentsTreeTitle, BorderLayout.WEST);

        licensesComponentsTree.expandRow(0);
        licensesComponentsTree.setRootVisible(false);
        licensesComponentsTree.setCellRenderer(new LicensesTreeCellRenderer());
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(licensesComponentsTree);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treeSpeedSearch.getComponent());
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_BAR_SCROLLING_UNITS);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, componentsTreePanel, treeScrollPane);

    }

    private JComponent createComponentsIssueDetailView() {
        issuesTable = new IssuesTable();
        JScrollPane tableScroll = ScrollPaneFactory.createScrollPane(issuesTable, SideBorder.ALL);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        JLabel title = new JBLabel(" Component Issues Details");
        title.setFont(title.getFont().deriveFont(TITLE_FONT_SIZE));
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, TITLE_LABEL_SIZE, title, tableScroll);
    }

    private JComponent createNoCredentialsView() {
        HyperlinkLabel link = new HyperlinkLabel();
        link.setHyperlinkText("To start using the JFrog Plugin, please ", "configure", " your JFrog Xray details.");
        link.addHyperlinkListener(e -> ShowSettingsUtil.getInstance().showSettingsDialog(project, XrayGlobalConfiguration.class));

        JBPanel panel = new JBPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(link, c);
        panel.setBackground(UIUtil.getTableBackground());
        return panel;
    }

    private void updateIssuesTable() {
        List<ScanTreeNode> selectedNodes = Lists.newArrayList((ScanTreeNode) issuesComponentsTree.getModel().getRoot());
        if (issuesComponentsTree.getSelectionPaths() != null) {
            selectedNodes.clear();
            TreePath[] selectedTreeNodes = issuesComponentsTree.getSelectionPaths();
            for (TreePath treePath : selectedTreeNodes) {
                selectedNodes.add((ScanTreeNode) treePath.getLastPathComponent());
            }
        }

        TableModel model = ScanManagerFactory.getScanManager(project).getFilteredScanIssues(selectedNodes);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        issuesTable.setRowSorter(sorter);
        issuesTable.setModel(model);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();

        resizeTableColumns();
        issuesTable.validate();
        issuesTable.repaint();
    }

    private void resizeTableColumns() {
        int tableWidth = issuesTable.getParent().getWidth();
        tableWidth -= (issuesTable.getColumnModel().getColumn(IssuesTableModel.IssueColumn.SEVERITY.ordinal()).getWidth());
        tableWidth -= (issuesTable.getColumnModel().getColumn(IssuesTableModel.IssueColumn.ISSUE_TYPE.ordinal()).getWidth());
        issuesTable.getColumnModel().getColumn(IssuesTableModel.IssueColumn.SUMMARY.ordinal()).setPreferredWidth((int) (tableWidth * 0.6));
        issuesTable.getColumnModel().getColumn(IssuesTableModel.IssueColumn.COMPONENT.ordinal()).setPreferredWidth((int) (tableWidth * 0.4));
    }

    @Override
    public void dispose() {

    }
}