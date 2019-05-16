package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.filter.FilterManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Severity;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class IssueFilterMenu extends FilterMenu<Severity> {

    public IssueFilterMenu(@NotNull Project project) {
        super(project);
        Map<Severity, Boolean> severitiesFilters =  FilterManager.getInstance().getSelectedSeverities();
        for (Severity severity : Severity.NEW_SEVERITIES) {
            severitiesFilters.put(severity, true);
        }
        addComponents(severitiesFilters, false);
    }
}