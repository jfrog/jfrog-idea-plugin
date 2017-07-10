package org.jfrog.idea.ui.xray.filters;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.messages.MessageBus;
import org.jfrog.idea.xray.FilterManager;
import org.jfrog.idea.Events;
import org.jfrog.idea.xray.scan.ScanManager;
import org.jfrog.idea.xray.ScanManagerFactory;
import org.jfrog.idea.xray.persistency.types.License;

/**
 * Created by romang on 4/13/17.
 */
public class LicenseFilterMenu extends FilterMenu {

    private final Project project;

    public LicenseFilterMenu(Project project) {
        super("Licenses filter:");
        this.project = project;
    }

    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        ScanManager scanManager = ScanManagerFactory.getScanManager(project);
        for (License license : scanManager.getAllLicenses()) {
            actionGroup.add(new CheckboxAction(StringUtil.toTitleCase(license.name)) {
                @Override
                public boolean isSelected(AnActionEvent e) {
                    try {
                        FilterManager filterManager = FilterManager.getInstance(e.getProject());
                        if (filterManager.selectedLicenses != null) {
                            return filterManager.selectedLicenses.contains(license);
                        }
                    } catch (NullPointerException e1) {

                    }
                    return false;
                }

                @Override
                public void setSelected(AnActionEvent e, boolean state) {
                    if (e.getProject() == null || e.getPresentation().getText() == null) {
                        return;
                    }

                    FilterManager filterManager = ServiceManager.getService(e.getProject(), FilterManager.class);
                    if (state) {
                        filterManager.selectedLicenses.add(license);
                    } else {
                        filterManager.selectedLicenses.remove(license);
                    }

                    MessageBus messageBus = e.getProject().getMessageBus();
                    messageBus.syncPublisher(Events.ON_SCAN_FILTER_CHANGE).update();
                }
            });
        }
        return actionGroup;
    }
}
