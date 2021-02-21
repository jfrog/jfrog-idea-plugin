package com.jfrog.ide.idea.ui.filters.filtermenu;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.ui.filters.filtermanager.CiFilterManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class CiIssueFilterMenu extends IssueFilterMenu {

    public CiIssueFilterMenu(@NotNull Project mainProject) {
        super(mainProject, CiFilterManager.getInstance(mainProject));
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_CI_FILTER_CHANGE;
    }
}