package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds the yellow bulb action - "Upgrade Version".
 *
 * @author michaels
 */
public abstract class UpgradeVersion implements LocalQuickFix, Iconable, HighPriorityAction {
    protected String componentName;
    protected String fixVersion;
    protected String issue;
    protected Logger log;
    protected Map<String, String> env;

    public UpgradeVersion(String componentName, String fixVersion, Collection<String> issue) {
        this.componentName = componentName;
        this.fixVersion = fixVersion;
        this.issue = issue.toString();
        this.log = Logger.getInstance();
        this.env = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
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
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(project, "Upgrading dependency...") {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                ApplicationManager.getApplication().invokeAndWait(() -> {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                upgradeComponentVersion(project, descriptor);
                                log.info("Upgraded " + componentName + " to version " + fixVersion + " successfully.");
                            } catch (IOException e) {
                                log.error("Failed while trying to upgrade component " + componentName + " to version " + fixVersion + ". Error: " + e);
                            }
                        });
                    descriptor.getPsiElement().getContainingFile().getVirtualFile().refresh(false, false);
                });
            }
        };
        ProgressManager.getInstance().run(scanAndUpdateTask);
    }

    abstract public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) throws IOException;
}