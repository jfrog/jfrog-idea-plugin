package com.jfrog.ide.idea.ui.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.jfrog.ide.common.configuration.ServerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.validateGlobalConfig;

/**
 * @author yahavi
 **/
@RunWith(Parameterized.class)
public class ConfigVerificationUtilsNegativeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ServerConfig.PolicyType.VULNERABILITIES, "bad project", ""},
                {ServerConfig.PolicyType.PROJECT, "", ""},
                {ServerConfig.PolicyType.PROJECT, "bad@project", ""},
                {ServerConfig.PolicyType.PROJECT, "bad project", ""},
                {ServerConfig.PolicyType.WATCHES, "", ""},
                {ServerConfig.PolicyType.WATCHES, "project", "watch 1"},
                {ServerConfig.PolicyType.WATCHES, "", "watch#1,watch-2"},
                {ServerConfig.PolicyType.WATCHES, "", ",watch-1,watch-2"},
                {ServerConfig.PolicyType.WATCHES, "", "watch-1,watch-2,"}
        });
    }

    private final ServerConfig.PolicyType policyType;
    private final String project;
    private final String watches;

    public ConfigVerificationUtilsNegativeTest(ServerConfig.PolicyType policyType, String project, String watches) {
        this.policyType = policyType;
        this.project = project;
        this.watches = watches;
    }

    @Test(expected = ConfigurationException.class)
    public void testValidateGlobalConfig() throws ConfigurationException {
        validateGlobalConfig(policyType, project, watches);
    }
}
