package com.jfrog.ide.idea.inspections;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.util.ui.tree.TreeUtil;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;

/**
 * Adds the yellow bulb action - "Show in dependencies tree".
 *
 * @author yahavi
 */
public class ShowInDependenciesTree implements LocalQuickFix, Iconable, HighPriorityAction {

    private final DependenciesTree node;
    private final String description;

    public ShowInDependenciesTree(DependenciesTree node, String description) {
        this.node = node;
        this.description = description;
    }

    @Override
    public Icon getIcon(int flags) {
        return IconUtils.load("jfrog_icon");
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return description;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        Utils.focusJFrogToolWindow(project);
        TreeUtil.selectInTree(project, node, true, ComponentsTree.getInstance(project), true);
    }
}