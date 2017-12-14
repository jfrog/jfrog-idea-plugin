package org.jfrog.idea.ui.xray.renderers;

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

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultTreeCellRenderer cellRenderer = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        tree.putClientProperty(TREE_TABLE_TREE_KEY, new JBTable());
        cellRenderer.setIcon(null);
        return cellRenderer;
    }
}