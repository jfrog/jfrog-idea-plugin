package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.go.GoComponentUpdater;
import com.jfrog.ide.idea.utils.GoUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Adds the yellow bulb action - ""Upgrade Version"".
 *
 * @author michaels
 */
public class GoUpgradeVersion extends UpgradeVersion {

    public GoUpgradeVersion(String componentName, String fixVersion, String issue) {
        super(componentName, fixVersion, issue);
    }

    @Override
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) throws IOException {
        String goExec = GoUtils.getGoExeAndSetEnv(env, project);
        GoComponentUpdater goComponentUpdater = new GoComponentUpdater(Paths.get(project.getBasePath()), this.log, this.env, goExec);
        goComponentUpdater.run(componentName, fixVersion);
    }
}