package com.jfrog.ide.idea.ui;

import com.intellij.openapi.ui.JBPopupMenu;
import com.jfrog.ide.idea.actions.CreateIgnoreRuleAction;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import static com.jfrog.ide.idea.ui.IssuesTableModel.IssueColumn.COMPONENT;
import static com.jfrog.ide.idea.ui.utils.ComponentUtils.replaceAndUpdateUI;

/**
 * Represents a click on the issues table.
 *
 * @author yahavi
 **/
class IssuesTableSelectionListener extends MouseAdapter implements ListSelectionListener {
    private final ComponentIssuesTable issuesTable;
    private final JPanel detailsPanel;

    IssuesTableSelectionListener(JPanel detailsPanel, ComponentIssuesTable issuesTable) {
        this.detailsPanel = detailsPanel;
        this.issuesTable = issuesTable;
    }

    /**
     * Show the "Create Ignore Rule" button after right-click on a violation.
     *
     * @param e - The mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (!SwingUtilities.isRightMouseButton(e)) {
            return;
        }
        int selectedRow = getSelectedRow(e);
        if (selectedRow == -1) {
            return;
        }

        DependencyTree impactedNode = getImpactedNode(selectedRow);
        if (impactedNode == null) {
            return;
        }
        Issue selectedIssue = issuesTable.getIssueAt(selectedRow);
        doRightClickButtonEvent(e, selectedIssue);
    }

    /**
     * Display the issue details view after selecting it by mouse or by keyboard.
     *
     * @param e - The selection event
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        int selectedRow = ((ListSelectionModel) e.getSource()).getMinSelectionIndex();
        if (selectedRow == -1) {
            return;
        }
        DependencyTree impactedNode = getImpactedNode(selectedRow);
        if (impactedNode == null) {
            return;
        }
        Issue selectedIssue = issuesTable.getIssueAt(selectedRow);
        replaceAndUpdateUI(detailsPanel, new IssueDetails(selectedIssue, impactedNode), BorderLayout.NORTH);
    }

    /**
     * Get the selected row in the table or -1 if no row selected.
     *
     * @param event - The mouse click event
     * @return the selected row or -1.
     */
    private int getSelectedRow(MouseEvent event) {
        int row = issuesTable.rowAtPoint(event.getPoint());
        if (row < 0 || row >= issuesTable.getRowCount()) {
            return -1;
        }
        return row;
    }

    /**
     * Get a node that contains the selected issue.
     * This method iterates over all subtree of the selected nodes in the dependency tree.
     *
     * @return a node that contains the selected issue.
     */
    private DependencyTree getImpactedNode(int selectedRow) {
        String selectedComponent = (String) issuesTable.getValueAt(selectedRow, COMPONENT.ordinal());
        return issuesTable.getSelectedNodes().stream()
                .map(node -> node.find(selectedComponent))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    /**
     * Display right click menu after a right click on an issue.
     *
     * @param mouseEvent    - The mouse event
     * @param selectedIssue - The selected issue
     */
    private void doRightClickButtonEvent(MouseEvent mouseEvent, Issue selectedIssue) {
        JPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setFocusable(false);
        popupMenu.add(new CreateIgnoreRuleAction(selectedIssue.getIgnoreRuleUrl(), mouseEvent));
        JBPopupMenu.showByEvent(mouseEvent, popupMenu);
    }
}
