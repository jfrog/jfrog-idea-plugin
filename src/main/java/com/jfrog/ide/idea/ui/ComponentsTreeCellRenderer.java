package com.jfrog.ide.idea.ui;

import com.intellij.ui.JBDefaultTreeCellRenderer;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class ComponentsTreeCellRenderer extends JBDefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultTreeCellRenderer cellRenderer = (JBDefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DependenciesTree scanTreeNode = (DependenciesTree) value;

        // Set icon
        Issue topIssue = scanTreeNode.getTopIssue();
        cellRenderer.setIcon(IconUtils.load(StringUtils.lowerCase(topIssue.getSeverity().toString())));

        // Add issues-count to tree node.
        if (scanTreeNode.getIssueCount() > 0) {
            cellRenderer.setText(scanTreeNode.toString() + " (" + scanTreeNode.getIssueCount() + ")");
        }

        return cellRenderer;
    }
}
