package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.go.GoComponentUpdater;
import com.jfrog.ide.idea.utils.GoUtils;
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
public class GoUpgradeVersion extends UpgradeVersion {
    private final String descriptorPath;

    public GoUpgradeVersion(String componentName, String fixVersion, Collection<String> issue, String descriptorPath) {
        super(componentName, fixVersion, issue);
        this.descriptorPath = descriptorPath;
    }

    @Override
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) throws IOException {
        Path modulePath = Paths.get(descriptorPath).getParent();
        String goExec = GoUtils.getGoExeAndSetEnv(env, project);
        GoComponentUpdater goComponentUpdater = new GoComponentUpdater(modulePath, this.log, this.env, goExec);
        goComponentUpdater.run(componentName, fixVersion);
    }
}
