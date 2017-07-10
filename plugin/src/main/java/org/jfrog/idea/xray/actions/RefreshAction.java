package org.jfrog.idea.xray.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jfrog.idea.xray.ScanManagerFactory;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        ScanManagerFactory.getScanManager(e.getProject()).asyncScanAndUpdateResults(false);
    }
}
