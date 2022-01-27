package com.jfrog.ide.idea.ui;

import com.google.common.collect.Lists;
import com.intellij.ui.table.JBTable;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.Severity;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jfrog.ide.idea.ui.IssuesTableModel.IssueColumn.*;

/**
 * @author yahavi
 */
public class ComponentIssuesTable extends JBTable {
    private List<DependencyTree> selectedNodes = Lists.newArrayList();

    private static final List<RowSorter.SortKey> SORT_KEYS = Lists.newArrayList(
            new RowSorter.SortKey(SEVERITY.ordinal(), SortOrder.DESCENDING),
            new RowSorter.SortKey(ISSUE_ID.ordinal(), SortOrder.DESCENDING),
            new RowSorter.SortKey(COMPONENT.ordinal(), SortOrder.ASCENDING));

    ComponentIssuesTable() {
        setModel(new IssuesTableModel());
        setShowGrid(true);
        setDefaultRenderer(Object.class, new IssuesTableCellRenderer());
        getTableHeader().setReorderingAllowed(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
    }

    public void updateIssuesTable(Set<Issue> selectedIssue, List<DependencyTree> selectedNodes) {
        this.selectedNodes = selectedNodes;
        Set<String> selectedNodeNames = selectedNodes.stream().map(DefaultMutableTreeNode::toString).collect(Collectors.toSet());
        TableModel model = new IssuesTableModel(selectedIssue, selectedNodeNames);
        TableRowSorter<TableModel> sorter = createTableRowSorter(model, selectedNodeNames);
        setModel(model);
        setRowSorter(sorter);
        resizeTableColumns();
        resizeAndRepaint();
    }

    /**
     * Add mouse click listener on the issues table.
     *
     * @param moreInfoPanel - The more info panel
     */
    public void addTableSelectionListener(JPanel moreInfoPanel) {
        addMouseListener(new IssuesTableSelectionListener(moreInfoPanel, this));
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

        TableColumn cveCol = getColumnModel().getColumn(ISSUE_ID.ordinal());
        cveCol.setPreferredWidth((int) (cveCol.getPreferredWidth() * 1.6));
        tableWidth -= cveCol.getPreferredWidth();

        getColumnModel().getColumn(COMPONENT.ordinal()).setPreferredWidth(tableWidth);
    }

    public List<DependencyTree> getSelectedNodes() {
        return selectedNodes;
    }
}
