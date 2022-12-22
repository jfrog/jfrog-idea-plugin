package com.jfrog.ide.idea.ui;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.HighlightableCellRenderer;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.common.tree.SubtitledTreeNode;
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

        if (!(scanTreeNode instanceof FileTreeNode)) {
            setText(scanTreeNode.getTitle());
            return cellRenderer;
        }
        String text = scanTreeNode.getTitle() + " " + scanTreeNode.getSubtitle();
        setText(text);

        // Set title style
        cellRenderer.addHighlighter(0, scanTreeNode.getTitle().length(), titleStyle);

        // Set subtitle style
        cellRenderer.addHighlighter(text.length() - scanTreeNode.getSubtitle().length(), text.length(), subtitleStyle);
        return cellRenderer;
    }
}
