package com.jfrog.ide.idea.ui.menus.filtermenu;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Severity;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class IssueFilterMenu extends FilterMenu<Severity> {

    public static final String TOOLTIP = "Select severities to show";
    public static final String NAME = "Severity";

    public IssueFilterMenu(@NotNull Project project, FilterManager filterManager) {
        super(project, NAME, TOOLTIP);
        Map<Severity, Boolean> severitiesFilters = filterManager.getSelectedSeverities();
        addComponents(severitiesFilters, false);
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_SCAN_FILTER_CHANGE;
    }
}