package org.jfrog.idea.ui.xray.filters;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.xray.FilterManager;
import org.jfrog.idea.xray.ScanManagerFactory;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.scan.ScanManager;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 23 Nov 2017.
 */
public class LicenseFilterMenu extends FilterMenu<License> {
    public LicenseFilterMenu(@NotNull Project project) {
        super(project);
    }

    public void setLicenses() {
        ScanManager scanManager = ScanManagerFactory.getScanManager(project);
        if (scanManager == null) {
            return;
        }
        Map<License, Boolean> selectedLicenses =  FilterManager.getInstance(project).selectedLicenses;
        scanManager.getAllLicenses().forEach(license -> {
            if (!selectedLicenses.containsKey(license)) {
                selectedLicenses.put(license, true);
            }
        });
        addComponents(selectedLicenses, true);
    }
}