package com.jfrog.ide.idea.inspections;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.tree.TreeUtil;
import com.jfrog.ide.common.tree.DependencyNode;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Represents the icon near the dependencies in the package descriptor file.
 *
 * @author yahavi
 **/
public class AnnotationIconRenderer extends GutterIconRenderer {
    private final DependencyNode node;
    private final String tooltipText;
    private final Icon icon;
    private Project project;

    public AnnotationIconRenderer(DependencyNode node, String tooltipText) {
        this.node = node;
        this.tooltipText = tooltipText;
        this.icon = IconUtils.load(StringUtils.lowerCase(node.getSeverity().toString()));
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AnnotationIconRenderer)) {
            return false;
        }
        return Objects.equals(icon, ((AnnotationIconRenderer) obj).getIcon());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(icon);
    }

    @Override
    public @NotNull Icon getIcon() {
        return icon;
    }

    @Override
    public @Nullable String getTooltipText() {
        return tooltipText;
    }

    @Override
    public @Nullable AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Utils.focusJFrogToolWindow(project);
                TreeUtil.selectInTree(project, node, true, LocalComponentsTree.getInstance(project), true);
            }
        };
    }
}
