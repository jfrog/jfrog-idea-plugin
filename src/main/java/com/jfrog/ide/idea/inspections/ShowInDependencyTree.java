package com.jfrog.ide.idea.inspections;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.util.ui.tree.TreeUtil;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;

/**
 * Adds the yellow bulb action - "Show in dependency tree".
 *
 * @author yahavi
 */
public class ShowInDependencyTree implements LocalQuickFix, Iconable, HighPriorityAction {

    private final DependencyTree node;
    private final String description;

    public ShowInDependencyTree(DependencyTree node, String description) {
        this.node = node;
        this.description = description;
    }

    @Override
    public Icon getIcon(int flags) {
        return IconUtils.load("jfrog_icon");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return description;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        Utils.focusJFrogToolWindow(project);
        TreeUtil.selectInTree(project, node, true, LocalComponentsTree.getInstance(project), true);
    }
}