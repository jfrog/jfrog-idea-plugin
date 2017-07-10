package org.jfrog.idea.ui.xray;

import org.jfrog.idea.ui.utils.IconUtils;
import org.jfrog.idea.xray.persistency.types.Issue;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Created by romang on 3/26/17.
 */
public class IssueTableCellRenderer implements TableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Issue issue = (Issue) value;
        JLabel summaryLabel = new JLabel(issue.summary);
        summaryLabel.setIconTextGap(8);
        summaryLabel.setIcon(IconUtils.load(issue.sevirity));
        summaryLabel.setOpaque(true);
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 0));
        if (isSelected) {
            summaryLabel.setForeground(table.getSelectionForeground());
            summaryLabel.setBackground(table.getSelectionBackground());
        } else {
            summaryLabel.setForeground(table.getForeground());
            summaryLabel.setBackground(table.getBackground());
        }
        return summaryLabel;
    }
}
