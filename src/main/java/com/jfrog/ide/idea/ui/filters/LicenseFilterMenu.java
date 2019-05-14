package com.jfrog.ide.idea.ui.filters;

import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.extractor.scan.License;

import java.util.Map;
import java.util.Set;

/**
 * Created by Yahav Itzhak on 23 Nov 2017.
 */
public class LicenseFilterMenu extends FilterMenu<License> {

    public void setLicenses() {
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers();
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }
        Map<License, Boolean> selectedLicenses = FilterManager.getInstance().getSelectedLicenses();
        scanManagers.forEach(scanManager ->
                scanManager.getAllLicenses()
                        .stream()
                        .filter(license -> !selectedLicenses.containsKey(license))
                        .forEach(license -> selectedLicenses.put(license, true)));
        addComponents(selectedLicenses, true);
    }
}