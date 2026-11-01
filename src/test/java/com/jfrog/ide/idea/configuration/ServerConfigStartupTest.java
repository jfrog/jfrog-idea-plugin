package com.jfrog.ide.idea.configuration;

import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.configuration.JfrogCliDriver;
import org.jfrog.build.api.util.Log;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Startup configuration must survive the credentials CI actually provides,
 * and a CLI probe that cannot log must not take settings down.
 */
public class ServerConfigStartupTest {

    @Test
    public void accessTokenEnvConfiguresXrayWithoutUsername() {
        try (MockedStatic<EnvironmentUtil> environment = Mockito.mockStatic(EnvironmentUtil.class)) {
            environment.when(() -> EnvironmentUtil.getValue(Mockito.anyString())).thenReturn(null);
            environment.when(() -> EnvironmentUtil.getValue(ServerConfigImpl.PLATFORM_URL_ENV)).thenReturn("https://example.jfrog.io/");
            environment.when(() -> EnvironmentUtil.getValue(ServerConfigImpl.ACCESS_TOKEN_ENV)).thenReturn("token");

            ServerConfigImpl serverConfig = new ServerConfigImpl();
            serverConfig.readConnectionDetailsFromEnv();

            assertTrue(serverConfig.isXrayConfigured());
            assertEquals("https://example.jfrog.io/", serverConfig.getUrl());
            assertEquals("https://example.jfrog.io/xray", serverConfig.getXrayUrl());
            assertEquals("https://example.jfrog.io/artifactory", serverConfig.getArtifactoryUrl());
            assertEquals("token", serverConfig.getAccessToken());
            assertEquals("", serverConfig.getUsername());
            assertEquals("", serverConfig.getPassword());
        }
    }

    @Test
    public void artifactoryUrlAndAccessTokenDoNotInventAnXrayUrl() {
        try (MockedStatic<EnvironmentUtil> environment = Mockito.mockStatic(EnvironmentUtil.class)) {
            environment.when(() -> EnvironmentUtil.getValue(Mockito.anyString())).thenReturn(null);
            environment.when(() -> EnvironmentUtil.getValue(ServerConfigImpl.ARTIFACTORY_URL_ENV)).thenReturn("https://example.jfrog.io/artifactory");
            environment.when(() -> EnvironmentUtil.getValue(ServerConfigImpl.ACCESS_TOKEN_ENV)).thenReturn("token");

            ServerConfigImpl serverConfig = new ServerConfigImpl();
            serverConfig.setUsername("stale-user");
            serverConfig.setPassword("stale-password");
            serverConfig.readConnectionDetailsFromEnv();

            assertEquals("", serverConfig.getXrayUrl());
            assertEquals("https://example.jfrog.io/artifactory", serverConfig.getArtifactoryUrl());
            assertEquals("", serverConfig.getUsername());
            assertEquals("", serverConfig.getPassword());
            assertEquals("token", serverConfig.getAccessToken());
            assertFalse(serverConfig.isXrayConfigured());
        }
    }

    @Test
    public void cliProbeWithNoLoggerDoesNotFailSettingsLoad() throws Exception {
        try (MockedStatic<EnvironmentUtil> environment = Mockito.mockStatic(EnvironmentUtil.class);
             MockedConstruction<JfrogCliDriver> ignored = Mockito.mockConstruction(JfrogCliDriver.class,
                     (mock, context) -> {
                         Log log = (Log) context.arguments().get(1);
                         when(mock.isJfrogCliInstalled()).thenAnswer(invocation -> {
                             if (log == null) {
                                 throw new NullPointerException("this.log is null");
                             }
                             return false;
                         });
                     })) {
            environment.when(EnvironmentUtil::getEnvironmentMap).thenReturn(new HashMap<>());

            assertFalse(new ServerConfigImpl().readConnectionDetailsFromJfrogCli());
        }
    }
}
