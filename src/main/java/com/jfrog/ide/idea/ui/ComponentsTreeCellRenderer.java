package com.jfrog.ide.idea.ui;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.HighlightableCellRenderer;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.SubtitledTreeNode;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class ComponentsTreeCellRenderer extends HighlightableCellRenderer {

    private static final TextAttributes titleStyle = new TextAttributes();
    private static final TextAttributes subtitleStyle = new TextAttributes();

    static {
        titleStyle.setFontType(JBFont.BOLD);
        subtitleStyle.setForegroundColor(UIUtil.getInactiveTextColor());
    }

    @Override
    public @NotNull Component getTreeCellRendererComponent(@NotNull JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        HighlightableCellRenderer cellRenderer = (HighlightableCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (!(value instanceof SubtitledTreeNode)) {
            return this;
        }

        SubtitledTreeNode scanTreeNode = (SubtitledTreeNode) value;
        if (scanTreeNode.getIcon() != null) {
            cellRenderer.setIcon(IconUtils.load(StringUtils.lowerCase(scanTreeNode.getIcon())));
        }

        String text = scanTreeNode.getTitle();
        int subtitleLength = 0;
        if (scanTreeNode.getSubtitle() != null && !scanTreeNode.getSubtitle().isEmpty()) {
            subtitleLength = scanTreeNode.getSubtitle().length();
            text += " " + scanTreeNode.getSubtitle();
        }
        setText(text);

        if (scanTreeNode instanceof FileTreeNode) {
            // Set title style
            cellRenderer.addHighlighter(0, scanTreeNode.getTitle().length(), titleStyle);
        }

        if (subtitleLength > 0) {
            // Set subtitle style
            cellRenderer.addHighlighter(text.length() - subtitleLength, text.length(), subtitleStyle);
        }

        return cellRenderer;
    }
}
