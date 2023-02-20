package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.yarn.YarnComponentUpdater;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) throws IOException {
        YarnComponentUpdater yarnComponentUpdater = new YarnComponentUpdater(Paths.get(project.getBasePath()), this.log, this.env);
        yarnComponentUpdater.run(componentName, fixVersion);
    }
}