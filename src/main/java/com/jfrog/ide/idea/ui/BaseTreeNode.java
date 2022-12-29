package com.jfrog.ide.idea.ui;

import com.jfrog.ide.common.tree.FileTreeNode;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The root node that is passed to the tree.
 * This node does not appear in the tree.
 * Its children appear as the upper level of the tree.
 */
public class BaseTreeNode extends DefaultMutableTreeNode {
    public void sortChildren() {
        children.sort((treeNode1, treeNode2) -> ((FileTreeNode) treeNode2).getTopSeverity().ordinal() - ((FileTreeNode) treeNode1).getTopSeverity().ordinal());
    }
}
