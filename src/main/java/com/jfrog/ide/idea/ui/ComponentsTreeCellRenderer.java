package com.jfrog.ide.idea.ui;

import com.intellij.ui.render.LabelBasedRenderer;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.Severity;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class ComponentsTreeCellRenderer extends LabelBasedRenderer.Tree {

    @Override
    public @NotNull Component getTreeCellRendererComponent(@NotNull JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        LabelBasedRenderer.Tree cellRenderer = (LabelBasedRenderer.Tree) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DependencyTree scanTreeNode = (DependencyTree) value;

        // Set icon
        Issue topIssue = scanTreeNode.getTopIssue();
        cellRenderer.setIcon(IconUtils.load(StringUtils.lowerCase(topIssue.getSeverity().toString())));

        // Add issues-count to tree node. We make sure the issues count is shown only on trees scanned by Xray,
        // by showing it only when the severity level is higher than unknown.
        if (scanTreeNode.getIssueCount() > 0 && topIssue.getSeverity().isHigherThan(Severity.Unknown)) {
            cellRenderer.setText(scanTreeNode + " (" + scanTreeNode.getIssueCount() + ")");
        }

        return cellRenderer;
    }
}
