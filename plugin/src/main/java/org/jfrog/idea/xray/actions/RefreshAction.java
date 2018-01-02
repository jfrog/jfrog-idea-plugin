package org.jfrog.idea.xray.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.util.messages.MessageBus;
import org.jfrog.idea.Events;
import org.jfrog.idea.xray.ScanManagerFactory;
import org.jfrog.idea.xray.scan.ScanManager;

/**
 * Created by romang on 3/6/17.
 */
public class RefreshAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() != null) {
            ScanManager scanManager = ScanManagerFactory.getScanManager(e.getProject());
            if (scanManager == null) {
                // Check if the project is supported now
                ScanManagerFactory scanManagerFactory = ServiceManager.getService(e.getProject(), ScanManagerFactory.class);
                scanManagerFactory.initScanManager(e.getProject());
                scanManager = ScanManagerFactory.getScanManager(e.getProject());
                if (scanManager == null) {
                    return;
                }
                MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
                messageBus.syncPublisher(Events.ON_IDEA_FRAMEWORK_CHANGE).update();
            }
            scanManager.asyncScanAndUpdateResults(false);
        }
    }
}