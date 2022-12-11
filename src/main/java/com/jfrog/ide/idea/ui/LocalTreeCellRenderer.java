package com.jfrog.ide.idea.ui;

import com.intellij.ui.render.RenderingUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.tree.SubtitledTreeNode;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class LocalTreeCellRenderer extends JPanel implements TreeCellRenderer {
    // TODO: remove
//    private DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();

    public LocalTreeCellRenderer() {
        super(new BorderLayout());
        add(title, BorderLayout.WEST);
        subtitle.setBorder(JBUI.Borders.emptyLeft(5));
//        subtitle.setEnabled(false); // TODO: remove?
        add(subtitle);
    }

    @Override
    public @NotNull Component getTreeCellRendererComponent(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        // TODO: old
//        LabelBasedRenderer.Tree cellRenderer = (LabelBasedRenderer.Tree) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        // TODO: new
        setComponentOrientation(tree.getComponentOrientation());
        setEnabled(tree.isEnabled());
        setFont(tree.getFont());
//        title.setText(value == null ? "" : value.toString());
//        title.setIcon(null);
//        subtitle.setText("subtitle");
//        subtitle.setIcon(null);

        // TODO: needed?
        setForeground(RenderingUtil.getForeground(tree, selected));
        setBackground(RenderingUtil.getBackground(tree, selected));
//        setBorder(EMPTY); // TODO: remove?
        title.setForeground(UIUtil.getTreeForeground(selected, hasFocus));
        if (selected && hasFocus) {
            subtitle.setForeground(UIUtil.getTreeSelectionForeground(true));
        } else {
            subtitle.setForeground(UIUtil.getInactiveTextColor());
        }
        // TODO:

//        JLabel l = (JLabel) cellRenderer.getTreeCellRendererComponent(
//                tree, value, selected, expanded, leaf, row, hasFocus);

        if (!(value instanceof SubtitledTreeNode)) {
            return this;
        }

        SubtitledTreeNode scanTreeNode = (SubtitledTreeNode) value;
//        DependencyTree depTreeNode = scanTreeNode.getVulnerabilityObj();

        // TODO: handle these:
//        // Set icon
//        Issue topIssue = depTreeNode.getTopIssue();
//        title.setIcon(IconUtils.load(StringUtils.lowerCase(topIssue.getSeverity().toString())));
//
//        // Add issues-count to tree node. We make sure the issues count is shown only on trees scanned by Xray,
//        // by showing it only when the severity level is higher than unknown.
//        if (depTreeNode.getIssueCount() > 0 && topIssue.getSeverity().isHigherThan(Severity.Unknown)) {
//            title.setText(scanTreeNode + " (" + depTreeNode.getIssueCount() + ")");
//        }
//
//        if (!depTreeNode.getViolatedLicenses().isEmpty()) {
//            title.setForeground(UIUtil.getErrorForeground());
//        }
        // TODO: after handling the above, remove the below:
        if (scanTreeNode.getIcon() != null) {
            title.setIcon(IconUtils.load(scanTreeNode.getIcon()));
        }
        title.setText(scanTreeNode.getTitle());
        subtitle.setText(scanTreeNode.getSubtitle());

        return this;
    }
}
