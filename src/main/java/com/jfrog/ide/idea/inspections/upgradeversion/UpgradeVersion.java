package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Adds the yellow bulb action - ""Upgrade Version"".
 *
 * @author michaels
 */
public abstract class UpgradeVersion implements LocalQuickFix, Iconable, HighPriorityAction {

    protected String componentName;
    protected String fixVersion;
    protected String issue;

    public UpgradeVersion(String componentName, String fixVersion, String issue) {
        this.componentName = componentName;
        this.fixVersion = fixVersion;
        this.issue = issue;
    }

    @Override
    public Icon getIcon(int flags) {
        return IconUtils.load("jfrog_icon");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Upgrade version to " + fixVersion + " to fix " + issue;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    }
}