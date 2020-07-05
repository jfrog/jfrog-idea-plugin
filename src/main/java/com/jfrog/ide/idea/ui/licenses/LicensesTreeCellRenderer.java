package com.jfrog.ide.idea.ui.licenses;

import com.intellij.ui.JBDefaultTreeCellRenderer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Created by Yahav Itzhak on 6 Dec 2017.
 */
public class LicensesTreeCellRenderer extends JBDefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultTreeCellRenderer cellRenderer = (JBDefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        // Remove the default icon
        cellRenderer.setIcon(null);
        return cellRenderer;
    }

}