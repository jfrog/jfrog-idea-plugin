package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Severity;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class IssueFilterMenu extends FilterMenu<Severity> {

    public static final String NAME = "Severity";
    public static final String TOOLTIP = "Select severities to show";

    public IssueFilterMenu(@NotNull Project mainProject) {
        super(mainProject, NAME, TOOLTIP);
        Map<Severity, Boolean> severitiesFilters = FilterManagerService.getInstance(mainProject).getSelectedSeverities();
        addComponents(severitiesFilters, false);
    }
}