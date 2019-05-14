package com.jfrog.ide.idea.ui.renderers;

import com.intellij.ui.JBDefaultTreeCellRenderer;
import com.intellij.ui.table.JBTable;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import static com.intellij.util.ui.tree.WideSelectionTreeUI.TREE_TABLE_TREE_KEY;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class IssuesTreeCellRenderer extends JBDefaultTreeCellRenderer {
    private static final JBTable EMPTY_TABLE = new JBTable();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultTreeCellRenderer cellRenderer = (JBDefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DependenciesTree scanTreeNode = (DependenciesTree) value;

        // Avoid setting TreeUnfocusedSelectionBackground
        tree.putClientProperty(TREE_TABLE_TREE_KEY, EMPTY_TABLE);

        // Set icon
        Issue topIssue = scanTreeNode.getTopIssue();
        cellRenderer.setIcon(IconUtils.load(StringUtils.lowerCase(topIssue.getSeverity().toString())));

        return cellRenderer;
    }
}
