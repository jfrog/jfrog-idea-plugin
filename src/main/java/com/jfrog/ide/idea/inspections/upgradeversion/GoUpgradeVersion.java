package com.jfrog.ide.idea.inspections.upgradeversion;

import com.google.common.collect.Maps;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.go.GoGet;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.utils.GoUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.Map;

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
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        Map<String, String> env = Maps.newHashMap(EnvironmentUtil.getEnvironmentMap());
        try {
            String goExec = GoUtils.getGoExeAndSetEnv(env, project);
            GoGet goGet = new GoGet(goExec, Paths.get(project.getBasePath()), env, Logger.getInstance());
            goGet.run(componentName, fixVersion);
        } catch (NoClassDefFoundError error) {
            Logger.getInstance().warn("Go plugin is not installed. Install it to get a better experience.");
        } catch (Exception e) {
            Logger.getInstance().warn("Failed while trying to upgrade Go component version. Error: " + e);
        }
    }

}