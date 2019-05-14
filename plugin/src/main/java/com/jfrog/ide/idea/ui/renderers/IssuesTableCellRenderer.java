package com.jfrog.ide.idea.ui.renderers;

import com.jfrog.ide.idea.ui.utils.IconUtils;
import com.jfrog.ide.idea.ui.models.IssuesTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Created by Yahav Itzhak on 13 Nov 2017.
 */
public class IssuesTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column == IssuesTableModel.IssueColumn.SEVERITY.ordinal()) {
            cellRenderer.setIcon(IconUtils.load(value.toString()));
            cellRenderer.setOpaque(true);
        } else {
            cellRenderer.setIcon(null);
        }

        return cellRenderer;
    }
}