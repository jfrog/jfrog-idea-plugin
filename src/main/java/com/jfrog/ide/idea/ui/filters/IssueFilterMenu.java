package com.jfrog.ide.idea.ui.filters;

import com.jfrog.ide.common.filter.FilterManager;
import org.jfrog.build.extractor.scan.Severity;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class IssueFilterMenu extends FilterMenu<Severity> {

    public IssueFilterMenu() {
        Map<Severity, Boolean> severitiesFilters =  FilterManager.getInstance().getSelectedSeverities();
        for (Severity severity : Severity.NEW_SEVERITIES) {
            severitiesFilters.put(severity, true);
        }
        addComponents(severitiesFilters, false);
    }
}