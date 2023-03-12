package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.jfrog.ide.idea.ui.configuration.JFrogGlobalConfiguration;

/**
 * Created by tala on 9/3/23.
 */
public class GoToSettingsAction extends AnAction {
    public GoToSettingsAction() {
        super("JFrog Global Configuration", "Go to JFrog global configuration ", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(null, JFrogGlobalConfiguration.class);
    }
}
