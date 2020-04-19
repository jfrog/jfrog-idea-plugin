package com.jfrog.ide.idea.ui.issues;

import com.google.common.collect.Lists;
import com.intellij.ui.table.JBTable;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.Severity;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.jfrog.ide.idea.ui.issues.IssuesTableModel.IssueColumn.*;

/**
 * @author yahavi
 */
class ComponentIssuesTable extends JBTable {

    private static final List<RowSorter.SortKey> SORT_KEYS = Lists.newArrayList(
            new RowSorter.SortKey(SEVERITY.ordinal(), SortOrder.DESCENDING),
            new RowSorter.SortKey(COMPONENT.ordinal(), SortOrder.ASCENDING));

    ComponentIssuesTable() {
        setModel(new IssuesTableModel());
        setShowGrid(true);
        setDefaultRenderer(Object.class, new IssuesTableCellRenderer());
        getTableHeader().setReorderingAllowed(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
    }

    void updateIssuesTable(Set<Issue> issueSet, Set<String> selectedComponents) {
        TableModel model = new IssuesTableModel(issueSet, selectedComponents);
        TableRowSorter<TableModel> sorter = createTableRowSorter(model, selectedComponents);
        setModel(model);
        setRowSorter(sorter);
        resizeTableColumns();
        resizeAndRepaint();
    }

    private TableRowSorter<TableModel> createTableRowSorter(TableModel model, Set<String> selectedComponents) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(SEVERITY.ordinal(), Comparator.comparing(o -> ((Severity) o)));

        // Add a comparator for 'Component' column, current-component's issues are shown before transitive-component's issues.
        Comparator<String> comparator = (s1, s2) -> {
            if (selectedComponents.contains(s1) && selectedComponents.contains(s2)) {
                return s1.compareTo(s2);
            }
            if (selectedComponents.contains(s1)) {
                return -1;
            }
            if (selectedComponents.contains(s2)) {
                return 1;
            }
            return s1.compareTo(s2);
        };
        sorter.setComparator(COMPONENT.ordinal(), comparator);

        sorter.setSortKeys(SORT_KEYS);
        sorter.sort();
        return sorter;
    }

    private void resizeTableColumns() {
        int tableWidth = getParent().getWidth();

        TableColumn severityCol = getColumnModel().getColumn(SEVERITY.ordinal());
        severityCol.setPreferredWidth(severityCol.getPreferredWidth() / 2);
        tableWidth -= severityCol.getPreferredWidth();

        TableColumn fixedVersionsCol = getColumnModel().getColumn(FIXED_VERSIONS.ordinal());
        fixedVersionsCol.setPreferredWidth((int) (fixedVersionsCol.getPreferredWidth() * 1.3));
        tableWidth -= fixedVersionsCol.getPreferredWidth();

        getColumnModel().getColumn(SUMMARY.ordinal()).setPreferredWidth((int) (tableWidth * 0.7));
        getColumnModel().getColumn(COMPONENT.ordinal()).setPreferredWidth((int) (tableWidth * 0.3));
    }
}
