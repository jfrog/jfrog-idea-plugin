package com.jfrog.ide.idea.ui.issues;

import com.intellij.openapi.application.ApplicationManager;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;
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

    private static IssuesTree instance;

    private IssuesTreeExpansionListener issuesTreeExpansionListener;
    private JLabel issuesCount;

    public static IssuesTree getInstance() {
        if (instance == null) {
            instance = new IssuesTree();
        }
        return instance;
    }

    private IssuesTree() {
        super();
        setCellRenderer(new IssuesTreeCellRenderer());
    }

    void setIssuesCountLabel(JLabel issuesCount) {
        this.issuesCount = issuesCount;
    }

    void createExpansionListener(JPanel issuesCountPanel, Map<TreePath, JPanel> issuesCountPanels) {
        issuesTreeExpansionListener = new IssuesTreeExpansionListener(this, issuesCountPanel, issuesCountPanels);
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
        FilterManager filterManager = FilterManager.getInstance();
        filterManager.applyFilters(project, filteredRoot, new DependenciesTree());
        filteredRoot.setIssues(filteredRoot.processTreeIssues());
        appendProjectToTree(filteredRoot);
        calculateIssuesCount();
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
