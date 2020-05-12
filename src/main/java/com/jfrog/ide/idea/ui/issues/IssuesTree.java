package com.jfrog.ide.idea.ui.issues;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.inspections.NavigationService;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @author yahavi
 */
public class IssuesTree extends BaseTree {

    private static final String POPUP_MENU_HEADLINE = "Show in project descriptor";
    private IssuesTreeExpansionListener issuesTreeExpansionListener;
    private JPanel issuesCountPanel;
    private JLabel issuesCount;
    private JBPopupMenu popupMenu = new JBPopupMenu();

    public static IssuesTree getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, IssuesTree.class);
    }

    private IssuesTree(@NotNull Project mainProject) {
        super(mainProject);
        setCellRenderer(new IssuesTreeCellRenderer());
    }

    void setIssuesCountLabel(JLabel issuesCount) {
        this.issuesCount = issuesCount;
    }

    void createExpansionListener(JPanel issuesCountPanel, Map<TreePath, JPanel> issuesCountPanels) {
        this.issuesCountPanel = issuesCountPanel;
        this.issuesTreeExpansionListener = new IssuesTreeExpansionListener(this, issuesCountPanel, issuesCountPanels);
    }

    void addTreeExpansionListener() {
        addTreeExpansionListener(issuesTreeExpansionListener);
    }

    public void populateTree(DependenciesTree root) {
        super.populateTree(root);
        issuesTreeExpansionListener.setIssuesCountPanel();
    }

    @Override
    public void addOnProjectChangeListener(MessageBusConnection busConnection) {
        busConnection.subscribe(ProjectEvents.ON_SCAN_PROJECT_ISSUES_CHANGE, this::applyFilters);
    }

    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectKey) {
        DependenciesTree project = projects.get(projectKey);
        if (project == null) {
            return;
        }
        DependenciesTree filteredRoot = (DependenciesTree) project.clone();
        filteredRoot.getIssues().clear();
        FilterManager filterManager = FilterManagerService.getInstance(mainProject);
        filterManager.applyFilters(project, filteredRoot, new DependenciesTree());
        filteredRoot.setIssues(filteredRoot.processTreeIssues());
        appendProjectWhenReady(filteredRoot);
        calculateIssuesCount();
    }

    @Override
    public void applyFiltersForAllProjects() {
        resetIssuesCountPanels();
        super.applyFiltersForAllProjects();
    }

    @Override
    public void reset() {
        super.reset();
        resetIssuesCountPanels();
    }

    private void resetIssuesCountPanels() {
        if (issuesCount != null && issuesCountPanel != null) {
            issuesCount.setText("Issues (0) ");
            issuesCountPanel.removeAll();
        }
    }

    private void calculateIssuesCount() {
        ApplicationManager.getApplication().invokeLater(() -> {
            DependenciesTree root = (DependenciesTree) getModel().getRoot();
            int sum = root.getChildren().stream()
                    .map(DependenciesTree::getIssues)
                    .distinct()
                    .flatMapToInt(issues -> IntStream.of(issues.size()))
                    .sum();
            issuesCount.setText("Issues (" + sum + ") ");
        });
    }

    public void addRightClickListener() {
        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleContextMenu(IssuesTree.this, e);
            }
        };
        addMouseListener(mouseListener);
    }

    private void handleContextMenu(IssuesTree tree, MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        // Event is right-click.
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null) {
            return;
        }
        createNodePopupMenu((DependenciesTree) selPath.getLastPathComponent());
        popupMenu.show(tree, e.getX(), e.getY());
    }

    private void createNodePopupMenu(DependenciesTree selectedNode) {
        popupMenu.removeAll();
        NavigationService navigationService = NavigationService.getInstance(mainProject);
        Set<PsiElement> navigationCandidates = navigationService.getNavigation(selectedNode);
        if (navigationCandidates == null) {
            // Find parent for navigation.
            selectedNode = navigationService.getNavigableParent(selectedNode);
            if (selectedNode == null) {
                return;
            }
            navigationCandidates = navigationService.getNavigation(selectedNode);
            if (navigationCandidates == null) {
                return;
            }
        }
        PsiElement navigationTarget = navigationCandidates.iterator().next();
        JMenuItem jumpToElement = new JBMenuItem(new AbstractAction(POPUP_MENU_HEADLINE) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(navigationTarget instanceof Navigatable)) {
                    return;
                }
                Navigatable navigatable = (Navigatable) navigationTarget;
                if (navigatable.canNavigate()) {
                    navigatable.navigate(true);
                }
            }
        });
        popupMenu.add(jumpToElement);
    }
}
