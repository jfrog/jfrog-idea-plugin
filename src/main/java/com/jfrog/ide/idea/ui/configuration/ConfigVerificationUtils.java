package com.jfrog.ide.idea.ui.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.jfrog.ide.common.configuration.ServerConfig;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.FileSystems;
import java.util.regex.PatternSyntaxException;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author yahavi
 **/
public class ConfigVerificationUtils {

    public static final String EXCLUSIONS_PREFIX = "**/*";
    public static final String EXCLUSIONS_SUFFIX = "*";
    public static String EXCLUSIONS_REGEX_PARSER = ".*\\{(.*)\\}\\*";
    public static final String DEFAULT_EXCLUSIONS = EXCLUSIONS_PREFIX + "{.idea,test, node_modules}" + EXCLUSIONS_SUFFIX;

    /**
     * Validate config project and watches before saving.
     *
     * @param policyType - The selected policy
     * @param project    - JFrog platform project key
     * @param watches    - Xray watches
     * @throws ConfigurationException if a field in the configuration is illegal.
     */
    static void validateGlobalConfig(String excludedPaths, ServerConfig.PolicyType policyType, String project, String watches) throws ConfigurationException {
        if (policyType == ServerConfig.PolicyType.PROJECT) {
            validateProject(project);
        }
        if (illegalCharactersExist(project)) {
            throw new ConfigurationException("Illegal characters in project key");
        }
        if (policyType == ServerConfig.PolicyType.WATCHES) {
            validateWatches(watches);
        }
        validateExcludedPaths(excludedPaths);
    }

    private static void validateExcludedPaths(String excludedPaths) throws ConfigurationException {
        if (StringUtils.isNotBlank(excludedPaths)) {
            if (!StringUtils.startsWith(excludedPaths, EXCLUSIONS_PREFIX)) {
                throw new ConfigurationException("Excluded paths pattern must start with " + EXCLUSIONS_PREFIX);
            }
            if (!StringUtils.endsWith(excludedPaths, EXCLUSIONS_SUFFIX)) {
                throw new ConfigurationException("Excluded paths pattern must end with " + EXCLUSIONS_SUFFIX);
            }
            try {
                FileSystems.getDefault().getPathMatcher("glob:" + excludedPaths);
            } catch (PatternSyntaxException e) {
                throw new ConfigurationException("Excluded paths pattern must be a valid glob pattern");
            }
        }
    }

    private static void validateProject(String project) throws ConfigurationException {
        if (isBlank(project)) {
            throw new ConfigurationException("Project key must be configured");
        }
    }

    private static void validateWatches(String watches) throws ConfigurationException {
        if (isBlank(watches)) {
            throw new ConfigurationException("Watches must be configured");
        }
        if (startsWith(watches, ",") || endsWith(watches, ",")) {
            throw new ConfigurationException("Watches list can't start or end with a delimiter");
        }
        for (String part : split(watches, ",")) {
            if (isBlank(part)) {
                throw new ConfigurationException("Watch can't be empty");
            }
            if (illegalCharactersExist(part)) {
                throw new ConfigurationException("Illegal characters in watch: " + part);
            }
        }
    }

    private static boolean illegalCharactersExist(String str) {
        return !str.matches("[\\w-.]*");
    }
}
