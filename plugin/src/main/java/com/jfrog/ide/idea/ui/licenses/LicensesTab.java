package com.jfrog.ide.idea.ui.licenses;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.ui.DetailsViewFactory;
import com.jfrog.ide.idea.ui.XrayToolWindow;
import com.jfrog.ide.idea.ui.components.FilterButton;
import com.jfrog.ide.idea.ui.components.TitledPane;
import com.jfrog.ide.idea.ui.filters.LicenseFilterMenu;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import java.awt.*;

/**
 * @author yahavi
 */
public class LicensesTab {

    private final Project project;

    private LicensesTree licensesTree = LicensesTree.getInstance();
    private OnePixelSplitter licensesCentralVerticalSplit;
    private LicenseFilterMenu licenseFilterMenu;
    private JScrollPane licensesDetailsScroll;
    private JPanel licensesDetailsPanel;

    public LicensesTab(Project project) {
        this.project = project;
    }

    public JPanel createLicenseInfoTab(boolean supported) {
        ActionToolbar toolbar = ComponentUtils.createActionToolbar(licensesTree);
        licenseFilterMenu = new LicenseFilterMenu(project);
        FilterButton licensesFilterButton = new FilterButton(licenseFilterMenu, "License", "Select licenses to show");
        licensesFilterButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        SimpleToolWindowPanel filterPanel = new SimpleToolWindowPanel(false);
        filterPanel.setToolbar(toolbar.getComponent());
        filterPanel.setContent(licensesFilterButton);
        licensesTree.setLicenseFilterMenu(licenseFilterMenu);

        JPanel licenseTab = new JBPanel(new BorderLayout());
        licensesCentralVerticalSplit = new OnePixelSplitter(false, 0.3f);
        licensesCentralVerticalSplit.setFirstComponent(createLicensesComponentsTreeView());
        licensesCentralVerticalSplit.setSecondComponent(createLicenseDetailsView(supported));
        licenseTab.add(filterPanel, BorderLayout.NORTH);
        licenseTab.add(licensesCentralVerticalSplit, BorderLayout.CENTER);
        return licenseTab;
    }

    private JComponent createLicenseDetailsView(boolean supported) {
        if (!GlobalSettings.getInstance().isCredentialsSet()) {
            return ComponentUtils.createNoCredentialsView(project);
        }
        if (!supported) {
            return ComponentUtils.createUnsupportedView();
        }
        JLabel title = new JBLabel(" Details");
        title.setFont(title.getFont().deriveFont(XrayToolWindow.TITLE_FONT_SIZE));

        licensesDetailsPanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        licensesDetailsPanel.add(ComponentUtils.createDisabledTextLabel("Select component or issue for more details"), BorderLayout.CENTER);
        licensesDetailsScroll = ScrollPaneFactory.createScrollPane(licensesDetailsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, XrayToolWindow.TITLE_LABEL_SIZE, title, licensesDetailsScroll);
    }

    private JComponent createLicensesComponentsTreeView() {
        JPanel componentsTreePanel = new JBPanel(new BorderLayout());
        componentsTreePanel.setBackground(UIUtil.getTableBackground());
        JLabel componentsTreeTitle = new JBLabel(" Components Tree");
        componentsTreeTitle.setFont(componentsTreeTitle.getFont().deriveFont(XrayToolWindow.TITLE_FONT_SIZE));
        componentsTreePanel.add(componentsTreeTitle, BorderLayout.WEST);

        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(licensesTree, ComponentUtils::getPathSearchString, true);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treeSpeedSearch.getComponent());
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(XrayToolWindow.SCROLL_BAR_SCROLLING_UNITS);
        return new TitledPane(JSplitPane.VERTICAL_SPLIT, XrayToolWindow.TITLE_LABEL_SIZE, componentsTreePanel, treeScrollPane);
    }

    public void onConfigurationChange() {
        licensesCentralVerticalSplit.setSecondComponent(createLicenseDetailsView(true));
    }

    public void populateTree() {
        licensesTree.populateTree(licensesTree.getModel());
//        DependenciesTree root = (DependenciesTree) issuesTreeModel.getRoot();
//        issuesCount.setText("Issues (" + root.getIssueCount() + ") ");
//        issuesTree.populateTree(issuesTreeModel);
    }

    public void registerListeners() {
        // License component selection listener
        licensesTree.addTreeSelectionListener(e -> {
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
            DetailsViewFactory.createLicenseDetailsView(licensesDetailsPanel, (DependenciesTree) e.getNewLeadSelectionPath().getLastPathComponent());
            // Scroll back to the beginning of the scrollable panel
            SwingUtilities.invokeLater(() -> licensesDetailsScroll.getViewport().setViewPosition(new Point()));
        });
    }
}
