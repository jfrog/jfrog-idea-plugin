package org.jfrog.idea.ui.xray;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.*;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.treeStructure.actions.CollapseAllAction;
import com.intellij.ui.treeStructure.actions.ExpandAllAction;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.Events;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.ui.configuration.XrayGlobalConfiguration;
import org.jfrog.idea.ui.utils.ComponentUtils;
import org.jfrog.idea.ui.xray.filters.IssueFilterMenu;
import org.jfrog.idea.ui.xray.filters.LicenseFilterMenu;
import org.jfrog.idea.xray.ScanManagerFactory;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.actions.FilterAction;
import org.jfrog.idea.xray.persistency.types.Issue;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by romang on 3/7/17.
 */
public class XrayToolWindow implements Disposable {

    private final Project project;

    private Tree componentsTree;
    private JBTable issuesTable;
    private JComponent issuesPanel;
    private JBPanel detailsPanel;
    private JScrollPane detailsScroll;
    private JBSplitter rightHorizontalSplit;


    XrayToolWindow(@NotNull Project project) {
        this.project = project;
    }

    void initToolWindow(@NotNull ToolWindow toolWindow, boolean supported) {
        ContentManager contentManager = toolWindow.getContentManager();
        if (!supported) {
            contentManager.addContent(createUnsupporteView());
            return;
        }

        contentManager.addContent(createContentView());
        registerListeners();
    }

    private Content createContentView() {
        OnePixelSplitter centralVerticalSplit = new OnePixelSplitter(false, 0.3f);
        rightHorizontalSplit = new JBSplitter(true, 0.7f);
        centralVerticalSplit.setFirstComponent(createComponentsView());

        issuesPanel = createIssuesView();
        if (GlobalSettings.getInstance().isCredentialsSet()) {
            rightHorizontalSplit.setFirstComponent(issuesPanel);
        } else {
            rightHorizontalSplit.setFirstComponent(createNoCredentialsView());
        }

        centralVerticalSplit.setSecondComponent(rightHorizontalSplit);
        rightHorizontalSplit.setSecondComponent(createDetailsView());

        JBPanel toolWindowView = new JBPanel(new BorderLayout());
        toolWindowView.add(createActionsToolbar(), BorderLayout.NORTH);
        toolWindowView.add(centralVerticalSplit, BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        return contentFactory.createContent(toolWindowView, "Xray Scan Results", false);
    }

    private Content createUnsupporteView() {
        JBPanel panel = new JBPanel(new BorderLayout());
        panel.setBackground(UIUtil.getTableBackground());
        panel.add(ComponentUtils.createDisabledTextLabel("Unsupported project type, currently only Maven projects are supported."), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        return contentFactory.createContent(panel, "unsupported project type", false);
    }

    private void registerListeners() {
        MessageBusConnection busConnection = project.getMessageBus().connect(project);
        // Xray credentials were set listener
        busConnection.subscribe(Events.ON_CONFIGURATION_DETAILS_CHANGE, ()
                -> ApplicationManager.getApplication().invokeLater(() -> {
            rightHorizontalSplit.setFirstComponent(issuesPanel);
            issuesPanel.validate();
            issuesPanel.repaint();
        }));

        // Component tree change listener
        busConnection.subscribe(Events.ON_SCAN_COMPONENTS_CHANGE, ()
                -> ApplicationManager.getApplication().invokeLater(() -> {
            TreeModel model = ScanManagerFactory.getScanManager(project).getFilteredScanTreeModel();
            componentsTree.setModel(model);
            componentsTree.validate();
            componentsTree.repaint();
        }));

        // Component selection listener
        componentsTree.addTreeSelectionListener(e -> {
            updateIssuesTable();
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            DetailsViewFactory.createDetailsView(detailsPanel, (ScanTreeNode) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            SwingUtilities.invokeLater(() -> detailsScroll.getViewport().setViewPosition(new Point(0, 0)));
        });

        // Issue selection listener
        issuesTable.getSelectionModel().addListSelectionListener(e -> {
            if (issuesTable.getSelectedRowCount() != 0) {
                Issue issue = (Issue) issuesTable.getValueAt(issuesTable.getSelectedRow(), issuesTable.getSelectedColumn());
                DetailsViewFactory.createDetailsView(detailsPanel, issue);
                // Scroll back to the beginning of the scrollable panel
                SwingUtilities.invokeLater(() -> detailsScroll.getViewport().setViewPosition(new Point(0, 0)));
            }
        });

        // Issues update listener
        busConnection.subscribe(Events.ON_SCAN_ISSUES_CHANGE, ()
                -> ApplicationManager.getApplication().invokeLater(this::updateIssuesTable));
    }

    private JComponent createDetailsView() {
        detailsPanel = new JBPanel(new BorderLayout());
        detailsPanel.setBackground(UIUtil.getTableBackground());
        detailsPanel.add(ComponentUtils.createDisabledTextLabel("Select component or issue for more details"), BorderLayout.CENTER);
        detailsScroll = ScrollPaneFactory.createScrollPane(detailsPanel, SideBorder.TOP);
        detailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return detailsScroll;
    }

    private JComponent createComponentsView() {
        componentsTree = new Tree(new ScanTreeNode(null));
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(componentsTree);
        return ScrollPaneFactory.createScrollPane(treeSpeedSearch.getComponent(), SideBorder.TOP);
    }

    private JComponent createIssuesView() {
        issuesTable = new JBTable();
        issuesTable.setDefaultRenderer(Object.class, new IssueTableCellRenderer());
        issuesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        issuesTable.setTableHeader(null);
        issuesTable.setShowGrid(false);

        JScrollPane tableScroll = ScrollPaneFactory.createScrollPane(issuesTable, SideBorder.TOP);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return tableScroll;
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
        ScanTreeNode selectedNode = (ScanTreeNode) componentsTree.getModel().getRoot();
        if (componentsTree.getSelectionPaths() != null && componentsTree.getSelectionPaths().length == 1) {
            selectedNode = (ScanTreeNode) componentsTree.getSelectionPaths()[0].getLastPathComponent();
        }

        TableModel model = ScanManagerFactory.getScanManager(project).getFilteredScanIssues(selectedNode);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        issuesTable.setRowSorter(sorter);
        issuesTable.setModel(model);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();

        issuesTable.validate();
        issuesTable.repaint();
    }

    private JComponent createActionsToolbar() {
        DefaultActionGroup mainGroup = new DefaultActionGroup();

        mainGroup.addAction(ActionManager.getInstance().getAction("Xray.Refresh"));
        mainGroup.add(new CollapseAllAction(componentsTree));
        mainGroup.add(new ExpandAllAction(componentsTree));
        mainGroup.addSeparator();
        mainGroup.add(new FilterAction(new IssueFilterMenu()));
        mainGroup.add(new FilterAction(new LicenseFilterMenu(project)));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, mainGroup, true);
        JPanel panel = new JPanel(new MigLayout("ins 0, fill", "[left]0[left, fill]push[right]", "center"));
        panel.add(toolbar.getComponent());
        return panel;
    }

    @Override
    public void dispose() {

    }
}
