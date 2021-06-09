package com.jfrog.ide.idea.ui.filters.filtermenu;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.ci.CiManager;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.ui.filters.filtermanager.CiFilterManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 23 Nov 2017.
 */
public class CiLicenseFilterMenu extends LicenseFilterMenu {

    public CiLicenseFilterMenu(@NotNull Project project) {
        super(project);
    }

    @Override
    public void refresh() {
        Map<License, Boolean> selectedLicenses = CiFilterManager.getInstance(project).getSelectedLicenses();
        CiManager.getInstance(project).getAllLicenses().stream()
                .filter(license -> !selectedLicenses.containsKey(license))
                .forEach(license -> selectedLicenses.put(license, true));
        addComponents(selectedLicenses, true);
        super.refresh();
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_CI_FILTER_CHANGE;
    }
}