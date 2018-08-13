package org.jfrog.idea.ui.xray.filters;

import com.intellij.openapi.project.Project;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.xray.FilterManager;
import org.jfrog.idea.xray.ScanManagersFactory;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.scan.ScanManager;

import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 23 Nov 2017.
 */
public class LicenseFilterMenu extends FilterMenu<License> {
    public LicenseFilterMenu(@NotNull Project project) {
        super(project);
    }

    public void setLicenses() {
        List<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(project);
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }
        Map<License, Boolean> selectedLicenses = FilterManager.getInstance(project).selectedLicenses;
        scanManagers.forEach(scanManager ->
                scanManager.getAllLicenses()
                        .stream()
                        .filter(selectedLicenses::containsKey)
                        .forEach(license -> selectedLicenses.put(license, true)));
        addComponents(selectedLicenses, true);
    }
}