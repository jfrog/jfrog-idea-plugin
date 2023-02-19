package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.yarn.YarnUpgrade;
import com.jfrog.ide.idea.log.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

/**
 * Adds the yellow bulb action - "Upgrade Version".
 *
 * @author michaels
 */
public class YarnUpgradeVersion extends UpgradeVersion {

    public YarnUpgradeVersion(String componentName, String fixVersion, String issue) {
        super(componentName, fixVersion, issue);
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        try {
            YarnUpgrade yarnUpgrade = new YarnUpgrade(Paths.get(project.getBasePath()), EnvironmentUtil.getEnvironmentMap());
            yarnUpgrade.run(componentName, fixVersion);
        } catch (Exception e) {
            Logger.getInstance().warn("Failed while trying to upgrade yarn component version. Error: " + e);
        }
    }
}