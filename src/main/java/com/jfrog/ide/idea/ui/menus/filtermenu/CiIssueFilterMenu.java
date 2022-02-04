package com.jfrog.ide.idea.ui.menus.filtermenu;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.ui.menus.filtermanager.CiFilterManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class CiIssueFilterMenu extends IssueFilterMenu {

    public CiIssueFilterMenu(@NotNull Project project) {
        super(project, CiFilterManager.getInstance(project));
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_CI_FILTER_CHANGE;
    }
}