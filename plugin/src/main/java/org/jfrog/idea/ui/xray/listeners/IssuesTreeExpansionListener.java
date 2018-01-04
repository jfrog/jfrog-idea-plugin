package org.jfrog.idea.ui.xray.listeners;

import com.intellij.ui.treeStructure.Tree;
import org.jfrog.idea.ui.utils.ComponentUtils;
import org.jfrog.idea.xray.ScanTreeNode;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 3 Jan 2018.
 */
public class IssuesTreeExpansionListener implements TreeExpansionListener {

    private Tree issuesComponentsTree;
    private JPanel issuesCountPanel;
    private Map<TreePath, JPanel> issuesCountPanels;

    public IssuesTreeExpansionListener(Tree issuesComponentsTree, JPanel issuesCountPanel, Map<TreePath, JPanel> issuesCountPanels) {
        this.issuesComponentsTree = issuesComponentsTree;
        this.issuesCountPanel = issuesCountPanel;
        this.issuesCountPanels = issuesCountPanels;
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        setIssuesCountPanel();
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        setIssuesCountPanel();
    }

    public void setIssuesCountPanel() {
        issuesCountPanel.removeAll();
        ScanTreeNode root = (ScanTreeNode) issuesComponentsTree.getModel().getRoot();
        setIssuesCountPanel(root, ComponentUtils.getTreePath(root));
    }

    private void setIssuesCountPanel(ScanTreeNode root, TreePath rootPath) {
        if (!issuesComponentsTree.isExpanded(rootPath)) {
            return;
        }
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
