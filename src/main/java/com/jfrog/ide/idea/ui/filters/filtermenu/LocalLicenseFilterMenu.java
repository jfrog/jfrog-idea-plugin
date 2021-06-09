package com.jfrog.ide.idea.ui.filters.filtermenu;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.ui.filters.filtermanager.LocalFilterManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;

import java.util.Map;
import java.util.Set;

/**
 * Created by Yahav Itzhak on 23 Nov 2017.
 */
public class LocalLicenseFilterMenu extends LicenseFilterMenu {

    public LocalLicenseFilterMenu(@NotNull Project project) {
        super(project);
    }

    @Override
    public void refresh() {
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(project);
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }
        Map<License, Boolean> selectedLicenses = LocalFilterManager.getInstance(project).getSelectedLicenses();
        scanManagers.forEach(scanManager ->
                scanManager.getAllLicenses()
                        .stream()
                        .filter(license -> !selectedLicenses.containsKey(license))
                        .forEach(license -> selectedLicenses.put(license, true)));
        addComponents(selectedLicenses, true);
        super.refresh();
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_SCAN_FILTER_CHANGE;
    }
}