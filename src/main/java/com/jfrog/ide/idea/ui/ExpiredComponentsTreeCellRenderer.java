package com.jfrog.ide.idea.ui;

import com.intellij.ui.HighlightableCellRenderer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ExpiredComponentsTreeCellRenderer extends ComponentsTreeCellRenderer {

    @Override
    public @NotNull Component getTreeCellRendererComponent(@NotNull JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        HighlightableCellRenderer cellRenderer = (HighlightableCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        cellRenderer.setForeground(UIUtil.isUnderDarcula() ? UIUtil.getHeaderInactiveColor() : UIUtil.getInactiveTextColor());
        return cellRenderer;
    }
}
