package org.jfrog.idea.ui.xray.filters;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.messages.MessageBus;
import org.jfrog.idea.xray.FilterManager;
import org.jfrog.idea.Events;
import org.jfrog.idea.xray.persistency.types.Severity;

/**
 * Created by romang on 4/13/17.
 */
public class IssueFilterMenu extends FilterMenu {

    public IssueFilterMenu() {
        super("Issues filter:");
    }

    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (Severity severity : Severity.values()) {
            actionGroup.add(new CheckboxAction(StringUtil.toTitleCase(severity.toString())) {
                @Override
                public boolean isSelected(AnActionEvent e) {
                    try {
                        FilterManager filterManager = FilterManager.getInstance(e.getProject());
                        if (filterManager.selectedSeverity != null) {
                            return filterManager.selectedSeverity.contains(Severity.valueOf(e.getPresentation().getText().toLowerCase()));
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
                        filterManager.selectedSeverity.add(Severity.valueOf(e.getPresentation().getText().toLowerCase()));
                    } else {
                        filterManager.selectedSeverity.remove(Severity.valueOf(e.getPresentation().getText().toLowerCase()));
                    }

                    MessageBus messageBus = e.getProject().getMessageBus();
                    messageBus.syncPublisher(Events.ON_SCAN_FILTER_CHANGE).update();
                }
            });
        }
        return actionGroup;
    }
}
