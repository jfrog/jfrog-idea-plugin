package com.jfrog.ide.idea.ui.licenses;

import com.intellij.ui.JBDefaultTreeCellRenderer;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import static com.intellij.util.ui.tree.WideSelectionTreeUI.TREE_TABLE_TREE_KEY;

/**
 * Created by Yahav Itzhak on 6 Dec 2017.
 */
public class LicensesTreeCellRenderer extends JBDefaultTreeCellRenderer {
    private static final JBTable emptyTable = new JBTable();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultTreeCellRenderer cellRenderer = (JBDefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        // Avoid setting TreeUnfocusedSelectionBackground
        tree.putClientProperty(TREE_TABLE_TREE_KEY, emptyTable);

        // Remove the default icon
        cellRenderer.setIcon(null);
        return cellRenderer;
    }

}