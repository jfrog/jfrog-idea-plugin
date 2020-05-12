package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Severity;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class IssueFilterMenu extends FilterMenu<Severity> {

    public IssueFilterMenu(@NotNull Project mainProject) {
        super(mainProject);
        Map<Severity, Boolean> severitiesFilters = FilterManagerService.getInstance(mainProject).getSelectedSeverities();
        addComponents(severitiesFilters, false, ApplicationEvents.ON_SCAN_FILTER_ISSUES_CHANGE);
    }
}