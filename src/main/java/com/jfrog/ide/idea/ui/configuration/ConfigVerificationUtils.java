package com.jfrog.ide.idea.ui.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.jfrog.ide.common.configuration.ServerConfig;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author yahavi
 **/
public class ConfigVerificationUtils {

    /**
     * Validate config project and watches before saving.
     *
     * @param policyType - The selected policy
     * @param project    - JFrog platform project key
     * @param watches    - Xray watches
     * @throws ConfigurationException if a field in the configuration is illegal.
     */
    static void validateGlobalConfig(ServerConfig.PolicyType policyType, String project, String watches) throws ConfigurationException {
        if (policyType == ServerConfig.PolicyType.PROJECT) {
            validateProject(project);
        }
        if (illegalCharactersExist(project)) {
            throw new ConfigurationException("Illegal characters in project key");
        }
        if (policyType == ServerConfig.PolicyType.WATCHES) {
            validateWatches(watches);
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
