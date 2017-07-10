package org.jfrog.idea.xray.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by romang on 4/13/17.
 */
public class FilterAction extends DumbAwareAction implements CustomComponentAction {

    @NotNull
    private final JComponent component;

    public FilterAction(@NotNull JComponent component) {
        this.component = component;
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        return component;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

    }
}
