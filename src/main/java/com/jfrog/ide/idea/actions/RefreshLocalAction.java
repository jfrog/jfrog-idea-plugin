package com.jfrog.ide.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.jfrog.ide.idea.scan.ScanManager;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshLocalAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }
        ScanManager.getInstance(e.getProject()).startScan();
    }
}
