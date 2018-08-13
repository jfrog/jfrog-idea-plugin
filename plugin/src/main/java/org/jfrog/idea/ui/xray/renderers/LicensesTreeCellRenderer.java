package org.jfrog.idea.ui.xray.renderers;

import com.intellij.ui.JBDefaultTreeCellRenderer;
import com.intellij.ui.table.JBTable;
import org.apache.commons.lang.StringUtils;
import org.jfrog.idea.xray.ScanTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import static com.intellij.util.ui.tree.WideSelectionTreeUI.TREE_TABLE_TREE_KEY;

/**
 * Created by Yahav Itzhak on 6 Dec 2017.
 */
public class LicensesTreeCellRenderer extends JBDefaultTreeCellRenderer {
    private static int originalFontSize;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultTreeCellRenderer cellRenderer = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (originalFontSize == 0) {
            originalFontSize = cellRenderer.getFont().getSize();
        }
        tree.putClientProperty(TREE_TABLE_TREE_KEY, new JBTable());
        cellRenderer.setIcon(null);
        Font font = cellRenderer.getFont();
        if (((ScanTreeNode) value).isModule()) {
            cellRenderer.setFont(new Font(font.getName(), Font.BOLD, originalFontSize + 1));
            return cellRenderer;
        }
        cellRenderer.setFont(new Font(font.getName(), Font.PLAIN, originalFontSize));
        return cellRenderer;
    }
}