package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.npm.NpmInstall;
import com.jfrog.ide.idea.log.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

/**
 * Adds the yellow bulb action - "Upgrade Version".
 *
 * @author michaels
 */
public class NpmUpgradeVersion extends UpgradeVersion {

    public NpmUpgradeVersion(String componentName, String fixVersion, String issue) {
        super(componentName, fixVersion, issue);
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        try {
            NpmInstall npmInstall = new NpmInstall(Paths.get(project.getBasePath()), EnvironmentUtil.getEnvironmentMap());
            npmInstall.run(componentName, fixVersion, Logger.getInstance());
        } catch (Exception e) {
            Logger.getInstance().warn("Failed while trying to upgrade npm component version. Error: " + e);
        }
    }
}