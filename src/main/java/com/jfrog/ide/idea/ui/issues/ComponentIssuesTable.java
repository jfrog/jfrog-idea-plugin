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

    private static final List<RowSorter.SortKey> SORT_KEYS = Lists.newArrayList(new RowSorter.SortKey(SEVERITY.ordinal(), SortOrder.DESCENDING));

    ComponentIssuesTable() {
        setModel(new IssuesTableModel());
        setShowGrid(true);
        setDefaultRenderer(Object.class, new IssuesTableCellRenderer());
        getTableHeader().setReorderingAllowed(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
    }

    void updateIssuesTable(Set<Issue> issueSet) {
        TableModel model = new IssuesTableModel(issueSet);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(SEVERITY.ordinal(), Comparator.comparing(o -> ((Severity) o)));
        setModel(model);
        setRowSorter(sorter);

        sorter.setSortKeys(SORT_KEYS);
        sorter.sort();

        resizeTableColumns();
        resizeAndRepaint();
    }

    private void resizeTableColumns() {
        int tableWidth = getParent().getWidth();
        tableWidth -= getColumnModel().getColumn(SEVERITY.ordinal()).getPreferredWidth();

        TableColumn fixedVersionsCol = getColumnModel().getColumn(FIXED_VERSIONS.ordinal());
        fixedVersionsCol.setPreferredWidth((int) (fixedVersionsCol.getPreferredWidth() * 1.1));
        tableWidth -= fixedVersionsCol.getPreferredWidth();

        getColumnModel().getColumn(SUMMARY.ordinal()).setPreferredWidth((int) (tableWidth * 0.6));
        getColumnModel().getColumn(COMPONENT.ordinal()).setPreferredWidth((int) (tableWidth * 0.4));
    }
}
