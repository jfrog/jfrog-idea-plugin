package com.jfrog.ide.idea.inspections;

import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;
import com.jfrog.ide.idea.inspections.upgradeversion.YarnUpgradeVersion;

import java.util.Collection;


/**
 * @author michaels
 */
public class YarnInspection extends NpmInspection {
    @Override
    UpgradeVersion getUpgradeVersion(String componentName, String fixVersion, Collection<String> issue) {
        return new YarnUpgradeVersion(componentName, fixVersion, issue);
    }
}
