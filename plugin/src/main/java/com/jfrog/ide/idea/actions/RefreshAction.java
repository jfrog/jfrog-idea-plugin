package com.jfrog.ide.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.jfrog.ide.idea.Events;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.Set;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }
        // Before we refresh the scanners, let's check if the project is supported.
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers();
        boolean scannersExistBeforeRefresh = CollectionUtils.isNotEmpty(scanManagers);
        ScanManagersFactory.getInstance().startScan(false);
        scanManagers = ScanManagersFactory.getScanManagers();
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }
        // The project was not supported before the refresh or it hasn't been initialised.
        if (!scannersExistBeforeRefresh) {
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(Events.ON_IDEA_FRAMEWORK_CHANGE).update();
        }

    }
}