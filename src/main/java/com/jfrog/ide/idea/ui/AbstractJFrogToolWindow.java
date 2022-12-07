package com.jfrog.ide.idea.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
import com.jfrog.ide.idea.ui.menus.filtermenu.IssueFilterMenu;
import com.jfrog.ide.idea.ui.menus.filtermenu.LicenseFilterMenu;
import com.jfrog.ide.idea.ui.menus.filtermenu.ScopeFilterMenu;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * @author yahavi
 */
public abstract class AbstractJFrogToolWindow extends SimpleToolWindowPanel implements Disposable {

    final MessageBusConnection projectBusConnection;
    final MessageBusConnection appBusConnection;
    final ComponentsTree componentsTree;
    final Project project;

    public AbstractJFrogToolWindow(@NotNull Project project, ComponentsTree componentsTree) {
        super(true);
        this.projectBusConnection = project.getMessageBus().connect(this);
        this.appBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        this.componentsTree = componentsTree;
        this.project = project;
    }

    /**
     * Create the action toolbar. That is the top toolbar.
     *
     * @return the action toolbar
     */
    abstract JPanel createActionToolbar();

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
     * Called after a change in the credentials.
     */
    public void onConfigurationChange() {
        resetViews();
    }

    /**
     * Clear the component tree.
     */
    void resetViews() {
        if (componentsTree != null) {
            componentsTree.reset();
        }
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the project bus connection
        projectBusConnection.disconnect();
        // Disconnect and release resources from the application bus connection
        appBusConnection.disconnect();
    }
}
