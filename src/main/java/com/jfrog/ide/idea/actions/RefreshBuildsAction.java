package com.jfrog.ide.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.jfrog.ide.idea.ci.CiManager;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshBuildsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }
        CiManager.getInstance(e.getProject()).asyncRefreshBuilds(false);
    }
}