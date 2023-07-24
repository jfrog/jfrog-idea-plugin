package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.yarn.YarnComponentUpdater;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Adds the yellow bulb action - "Upgrade Version".
 *
 * @author michaels
 */
public class YarnUpgradeVersion extends UpgradeVersion {
    private final String descriptorPath;

    public YarnUpgradeVersion(String componentName, String fixVersion, Collection<String> issue, String descriptorPath) {
        super(componentName, fixVersion, issue);
        this.descriptorPath = descriptorPath;
    }

    @Override
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) throws IOException {
        Path modulePath = Paths.get(descriptorPath).getParent();
        YarnComponentUpdater yarnComponentUpdater = new YarnComponentUpdater(modulePath, this.log, this.env);
        yarnComponentUpdater.run(componentName, fixVersion);
    }
}