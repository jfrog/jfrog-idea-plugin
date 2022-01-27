package com.jfrog.ide.idea.ui;

import com.intellij.openapi.ui.JBPopupMenu;
import com.jfrog.ide.idea.actions.CreateIgnoreRuleAction;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import static com.jfrog.ide.idea.ui.IssuesTableModel.IssueColumn.COMPONENT;
import static com.jfrog.ide.idea.ui.IssuesTableModel.IssueColumn.ISSUE_ID;
import static com.jfrog.ide.idea.ui.utils.ComponentUtils.replaceAndUpdateUI;

/**
 * Represents a click on the issues table.
 *
 * @author yahavi
 **/
class IssuesTableSelectionListener extends MouseAdapter {
    private final ComponentIssuesTable issuesTable;
    private final JPanel detailsPanel;

    IssuesTableSelectionListener(JPanel detailsPanel, ComponentIssuesTable issuesTable) {
        this.detailsPanel = detailsPanel;
        this.issuesTable = issuesTable;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int selectedRow = getSelectedRow(e);
        if (selectedRow == -1) {
            return;
        }

        DependencyTree impactedNode = getImpactedNode(selectedRow);
        if (impactedNode == null) {
            return;
        }
        Issue selectedIssue = getSelectedIssue(impactedNode, selectedRow);
        if (selectedIssue == null) {
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            doLeftClickButtonEvent(selectedIssue, impactedNode);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            doRightClickButtonEvent(e, selectedIssue);
        }
    }

    /**
     * Get the selected row if the user clicked on the right or the left mouse buttons.
     *
     * @param event - The mouse click event
     * @return the selected row.
     */
    private int getSelectedRow(MouseEvent event) {
        int row = issuesTable.rowAtPoint(event.getPoint());
        if (row < 0 || row >= issuesTable.getRowCount()) {
            return -1;
        }
        if (SwingUtilities.isMiddleMouseButton(event)) {
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
     * Extract the issue selected in the table from the dependency tree.
     *
     * @param impactedNode - The impacted node
     * @return the issue selected in the table.
     */
    private Issue getSelectedIssue(DependencyTree impactedNode, int selectedRow) {
        String selectedIssueStr = (String) issuesTable.getValueAt(selectedRow, ISSUE_ID.ordinal());
        return impactedNode.getIssues().stream()
                .filter(issue -> StringUtils.equals(issue.getIssueId(), selectedIssueStr))
                .findAny().orElse(null);
    }

    /**
     * Display the issue view after a left click on an issue.
     *
     * @param selectedIssue - The selected issue
     * @param impactedNode  - The impacted node
     */
    private void doLeftClickButtonEvent(Issue selectedIssue, DependencyTree impactedNode) {
        replaceAndUpdateUI(detailsPanel, new IssueDetails(selectedIssue, impactedNode), BorderLayout.NORTH);
    }

    /**
     * Display right click menu after a right click on an issue.
     *
     * @param mouseEvent    - The mouse event
     * @param selectedIssue - The selected issue
     */
    private void doRightClickButtonEvent(MouseEvent mouseEvent, Issue selectedIssue) {
        if (StringUtils.isBlank(selectedIssue.getIgnoreUrl())) {
            return;
        }
        JPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.setFocusable(false);
        popupMenu.add(new CreateIgnoreRuleAction(selectedIssue));
        JBPopupMenu.showByEvent(mouseEvent, popupMenu);
    }
}
