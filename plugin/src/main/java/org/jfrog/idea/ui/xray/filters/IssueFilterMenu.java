package org.jfrog.idea.ui.xray.filters;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.xray.FilterManager;
import org.jfrog.idea.xray.persistency.types.Severity;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class IssueFilterMenu extends FilterMenu<Severity> {

    public IssueFilterMenu(@NotNull Project project) {
        super(project);
        Map<Severity, Boolean> severitiesFilters =  FilterManager.getInstance(project).selectedSeverities;
        for (Severity severity : Severity.NEW_SEVERITIES) {
            severitiesFilters.put(severity, true);
        }
        addComponents(severitiesFilters, false);
    }
}