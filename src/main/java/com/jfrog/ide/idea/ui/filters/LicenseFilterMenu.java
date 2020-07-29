package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;

import java.util.Map;
import java.util.Set;

/**
 * Created by Yahav Itzhak on 23 Nov 2017.
 */
public class LicenseFilterMenu extends FilterMenu<License> {

    public static final String NAME = "License";
    public static final String TOOLTIP = "Select licenses to show";

    public LicenseFilterMenu(@NotNull Project mainProject) {
        super(mainProject, NAME, TOOLTIP);
    }

    @Override
    public void refresh() {
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(mainProject);
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }
        Map<License, Boolean> selectedLicenses = FilterManagerService.getInstance(mainProject).getSelectedLicenses();
        scanManagers.forEach(scanManager ->
                scanManager.getAllLicenses()
                        .stream()
                        .filter(license -> !selectedLicenses.containsKey(license))
                        .forEach(license -> selectedLicenses.put(license, true)));
        addComponents(selectedLicenses, true);
        super.refresh();
    }
}