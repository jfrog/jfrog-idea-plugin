package com.jfrog.ide.idea.configuration;

import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.configuration.JfrogCliDriver;
import org.apache.commons.io.FileUtils;
import org.gradle.internal.impldep.org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author yahavi
 **/
@RunWith(Parameterized.class)
public class ConnectionDetailsFromCliTest {

    private final String[] cliParameters;
    private final Exception exception;
    private final boolean expected;

    public ConnectionDetailsFromCliTest(String[] cliParameters, Exception exception, boolean expected) {
        this.cliParameters = cliParameters;
        this.exception = exception;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> dataProvider() {
        return Arrays.asList(new Object[][]{
                // No JFrog CLI config
                {null, new IOException("jfrog config export command failed. That might be happen if you haven't config any CLI server yet or using the config encryption feature."), false},

                // URLs without credentials
                {new String[]{"c", "add", "--xray-url=http://127.0.0.1"}, null, false},
                {new String[]{"c", "add", "--artifactory-url=http://127.0.0.1"}, null, false},

                // Credentials without URLs
                {new String[]{"c", "add", "--user=admin"}, null, false},
                {new String[]{"c", "add", "--user=admin", "--password=password"}, null, false},
                {new String[]{"c", "add", "--access-token=123"}, null, false},

                // Partial URLs
                {new String[]{"c", "add", "--artifactory-url=http://127.0.0.1:8081/artifactory", "--user=admin", "--password=password", "--enc-password=false"}, null, false},
                {new String[]{"c", "add", "--xray-url=http://127.0.0.1:8081/xray", "--user=admin", "--password=password", "--enc-password=false"}, null, false},

                // Positive tests
                {new String[]{"c", "add", "--url=http://127.0.0.1:8081", "--user=admin", "--password=password", "--enc-password=false"}, null, true},
                {new String[]{"c", "add", "--url=http://127.0.0.1:8081", "--access-token=123"}, null, true},
        });
    }

    @Test
    public void testReadConnectionDetailsFromJfrogCli() throws IOException, InterruptedException {
        Path jfrogCliHome = Files.createTempDirectory("testReadConnectionDetailsFromJfrogCli");
        try (MockedStatic<EnvironmentUtil> mockController = Mockito.mockStatic(EnvironmentUtil.class)) {
            // Set environment variables
            Map<String, String> envVars = new HashMap<>(System.getenv()) {{
                put("JFROG_CLI_HOME_DIR", jfrogCliHome.toAbsolutePath().toString());
                put("CI", "true");
            }};
            mockController.when(EnvironmentUtil::getEnvironmentMap).thenReturn(envVars);

            // Config JFrog CLI
            if (cliParameters != null) {
                JfrogCliDriver jfrogCliDriver = new JfrogCliDriver(envVars);
                jfrogCliDriver.runCommand(null, cliParameters, new ArrayList<>(), null);
            }

            // Check results
            ServerConfigImpl serverConfig = new ServerConfigImpl();
            if (exception != null) {
                Assert.assertThrows(exception.getMessage(), exception.getClass(), serverConfig::readConnectionDetailsFromJfrogCli);
            } else {
                assertEquals(expected, serverConfig.readConnectionDetailsFromJfrogCli());
            }
        } finally {
            FileUtils.forceDelete(jfrogCliHome.toFile());
        }
    }
}
