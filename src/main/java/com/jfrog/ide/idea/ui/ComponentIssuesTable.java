package com.jfrog.ide.idea.ui;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.table.JBTable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.Severity;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.jfrog.ide.idea.ui.IssuesTableModel.IssueColumn.*;

/**
 * @author yahavi
 */
public class ComponentIssuesTable extends JBTable {

    private static final List<RowSorter.SortKey> SORT_KEYS = Lists.newArrayList(
            new RowSorter.SortKey(SEVERITY.ordinal(), SortOrder.DESCENDING),
            new RowSorter.SortKey(COMPONENT.ordinal(), SortOrder.ASCENDING));

    ComponentIssuesTable() {
        setModel(new IssuesTableModel());
        setShowGrid(true);
        setDefaultRenderer(Object.class, new IssuesTableCellRenderer());
        getTableHeader().setReorderingAllowed(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
        setRightClickMenu();
    }

    public void updateIssuesTable(Set<Issue> issueSet, Set<String> selectedComponents) {
        TableModel model = new IssuesTableModel(issueSet, selectedComponents);
        TableRowSorter<TableModel> sorter = createTableRowSorter(model, selectedComponents);
        setModel(model);
        setRowSorter(sorter);
        resizeTableColumns();
        resizeAndRepaint();
    }

    /**
     * Sort rows by columns:
     * 1. Severity - from high to low.
     * 2. Component - direct before transitive issues.
     */
    private TableRowSorter<TableModel> createTableRowSorter(TableModel model, Set<String> selectedComponents) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(SEVERITY.ordinal(), Comparator.comparing(o -> ((Severity) o)));
        sorter.setComparator(COMPONENT.ordinal(), Comparator
                .comparing(s -> selectedComponents.contains(s.toString()) ? -1 : 0)
                .thenComparing(s -> (String) s));
        sorter.setSortKeys(SORT_KEYS);
        sorter.sort();
        return sorter;
    }

    private void resizeTableColumns() {
        int tableWidth = getParent().getWidth();

        TableColumn severityCol = getColumnModel().getColumn(SEVERITY.ordinal());
        severityCol.setPreferredWidth(severityCol.getPreferredWidth() / 2);
        tableWidth -= severityCol.getPreferredWidth();

        TableColumn cveCol = getColumnModel().getColumn(CVE.ordinal());
        cveCol.setPreferredWidth((int) (cveCol.getPreferredWidth() * 1.6));
        tableWidth -= cveCol.getPreferredWidth();

        TableColumn fixedVersionsCol = getColumnModel().getColumn(FIXED_VERSIONS.ordinal());
        fixedVersionsCol.setPreferredWidth((int) (fixedVersionsCol.getPreferredWidth() * 1.3));
        tableWidth -= fixedVersionsCol.getPreferredWidth();

        getColumnModel().getColumn(SUMMARY.ordinal()).setPreferredWidth((int) (tableWidth * 0.7));
        getColumnModel().getColumn(COMPONENT.ordinal()).setPreferredWidth((int) (tableWidth * 0.3));
    }

    private void setRightClickMenu() {
        JPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setFocusable(false);
        popupMenu.add(new CopyCve());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                if (row < 0 || row >= getRowCount()) {
                    // Click outside the table
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    // Select row on right click
                    setRowSelectionInterval(row, row);
                    if (StringUtils.isNotBlank((String) getValueAt(getSelectedRow(), CVE.ordinal()))) {
                        // Popup menu if CVE exist
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    public class CopyCve extends AbstractAction {
        public CopyCve() {
            super("Copy CVE", AllIcons.Actions.Copy);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(new StringSelection((String) getValueAt(getSelectedRow(), CVE.ordinal())), null);
        }
    }
}
