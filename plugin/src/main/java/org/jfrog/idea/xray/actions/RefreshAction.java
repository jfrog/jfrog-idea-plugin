package org.jfrog.idea.xray.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.util.messages.MessageBus;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.idea.Events;
import org.jfrog.idea.xray.ScanManagersFactory;
import org.jfrog.idea.xray.scan.ScanManager;

import java.util.List;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() != null) {
            List<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(e.getProject());
            if (CollectionUtils.isEmpty(scanManagers)) {
                // Check if the project is supported now
                ScanManagersFactory scanManagersFactory = ServiceManager.getService(e.getProject(), ScanManagersFactory.class);
                scanManagersFactory.initScanManagers(e.getProject());
                scanManagers = ScanManagersFactory.getScanManagers(e.getProject());
                if (CollectionUtils.isEmpty(scanManagers)) {
                    return;
                }
                MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
                messageBus.syncPublisher(Events.ON_IDEA_FRAMEWORK_CHANGE).update();
            }
            scanManagers.forEach(scanManager -> scanManager.asyncScanAndUpdateResults(false));
        }
    }
}