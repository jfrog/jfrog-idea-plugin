package com.jfrog.ide.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.jfrog.ide.idea.scan.ScanManagersFactory;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }
        ScanManagersFactory.getInstance(e.getProject()).startScan(false, null, null);
    }
}