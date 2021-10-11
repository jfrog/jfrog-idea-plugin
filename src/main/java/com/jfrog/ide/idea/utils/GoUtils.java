package com.jfrog.ide.idea.utils;

import com.goide.GoConstants;
import com.goide.sdk.GoSdkUtil;
import com.goide.vgo.configuration.VgoProjectSettings;
import com.google.common.collect.Lists;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author yahavi
 **/
public class GoUtils {

    private static final List<String> GO_RELEVANT_ENV = Lists.newArrayList("GOPROXY", "GONOPROXY", "GOPRIVATE", "GOSUMDB");

    /**
     * Retrieve and set "GO_PATH", "GOPROXY", "GOPRIVATE", "GONOPROXY", "GOSUMDB" from the Go plugin configuration.
     * Extract and return the Go executable path.
     *
     * @param env     - The environment variables map
     * @param project - Intellij project
     * @return Go executable path or null.
     * @throws NoClassDefFoundError if the Go plugin is not installed.
     */
    public static String getGoExeAndSetEnv(Map<String, String> env, Project project) throws NoClassDefFoundError {
        String goPath = GoSdkUtil.retrieveGoPath(project, ModuleManager.getInstance(project).getModules()[0]);
        if (StringUtils.isNotBlank(goPath)) {
            env.put(GoConstants.GO_PATH, goPath);
        }
        Map<String, String> currentConfiguration = VgoProjectSettings.getInstance(project).getEnvironment();
        GO_RELEVANT_ENV.forEach(envKey -> {
            String envValue = currentConfiguration.get(envKey);
            if (StringUtils.isNotBlank(envValue)) {
                env.put(envKey, envValue);
            }
        });
        String goExecutablePath = GoSdkUtil.retrieveEnvironmentPathForGo(project, null);
        if (StringUtils.isNotBlank(goExecutablePath)) {
            // The returned value may contain more than one path, seperated by ':' or ';'
            goExecutablePath = StringUtils.substringBefore(goExecutablePath, File.pathSeparator);
            return Paths.get(goExecutablePath, "go").toString();
        }
        return null;
    }

}
