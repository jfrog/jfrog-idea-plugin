package org.jfrog.idea.xray.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.idea.Events;
import org.jfrog.idea.xray.ScanManagersFactory;
import org.jfrog.idea.xray.scan.ScanManager;

import java.util.Set;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() != null) {
            // Check if the project was supported
            Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(e.getProject());
            boolean areScannersExistsBeforeRefresh = CollectionUtils.isNotEmpty(scanManagers);
            ScanManagersFactory.refreshScanManagers(e.getProject());
            scanManagers = ScanManagersFactory.getScanManagers(e.getProject());
            if (CollectionUtils.isEmpty(scanManagers)) {
                return;
            }
            // Case project was not supported or did not initialized
            if (!areScannersExistsBeforeRefresh) {
                MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
                messageBus.syncPublisher(Events.ON_IDEA_FRAMEWORK_CHANGE).update();
            }
            scanManagers.forEach(scanManager -> scanManager.asyncScanAndUpdateResults(false));
        }
    }
}