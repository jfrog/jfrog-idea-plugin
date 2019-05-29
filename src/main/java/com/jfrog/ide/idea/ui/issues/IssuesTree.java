package com.jfrog.ide.idea.ui.issues;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author yahavi
 */
public class IssuesTree extends BaseTree {

    private IssuesTreeExpansionListener issuesTreeExpansionListener;
    private JPanel issuesCountPanel;
    private JLabel issuesCount;

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

    public void populateTree(TreeModel issuesTreeModel) {
        super.populateTree(issuesTreeModel);
        issuesTreeExpansionListener.setIssuesCountPanel();
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
        appendProjectToTree(filteredRoot);
        calculateIssuesCount();
    }

    @Override
    public void reset() {
        super.reset();
        issuesCount.setText("Issues (0) ");
        issuesCountPanel.removeAll();
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
}
