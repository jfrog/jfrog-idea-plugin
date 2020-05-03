package com.jfrog.ide.idea.ui.issues;

import com.jfrog.ide.idea.ui.utils.IconUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Set;

/**
 * Created by Yahav Itzhak on 13 Nov 2017.
 */
public class IssuesTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Severity column.
        if (column == IssuesTableModel.IssueColumn.SEVERITY.ordinal()) {
            editSeverityColumn(cellRenderer, value);
        } else {
            cellRenderer.setIcon(null);
            cellRenderer.setHorizontalAlignment(JLabel.LEADING);
        }

        // Bold cell if current issue is direct.
        boldIfDirectIssue(cellRenderer, table, row);

        return cellRenderer;
    }

    /**
     * Severity column should present only the severity icon.
     */
    private static void editSeverityColumn(DefaultTableCellRenderer cellRenderer, Object value) {
        cellRenderer.setIcon(IconUtils.load(value.toString()));
        cellRenderer.setOpaque(true);
        cellRenderer.setToolTipText(value.toString());
        cellRenderer.setText("");
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);
    }

    private static void boldIfDirectIssue(DefaultTableCellRenderer cellRenderer, JTable table, int row) {
        // As row order may change due to sorting, get actual row index.
        int actualRow = table.getRowSorter().convertRowIndexToModel(row);

        Set<String> components = ((IssuesTableModel)table.getModel()).getComponents();
        String currentComponent = (String) table.getModel().getValueAt(actualRow, IssuesTableModel.IssueColumn.COMPONENT.ordinal());
        if (components.contains(currentComponent)) {
            Font bold = cellRenderer.getFont().deriveFont(Font.BOLD);
            cellRenderer.setFont(bold);
        }
    }
}