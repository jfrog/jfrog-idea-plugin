/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.jfrog.ide.idea.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author yahavi
 */
@Getter
@State(name = "GlobalSettings", storages = {@Storage("jfrogConfig.xml")})
public final class GlobalSettings implements PersistentStateComponent<GlobalSettings> {
    private ServerConfigImpl serverConfig;

    @SuppressWarnings("unused")
    GlobalSettings() {
        this.serverConfig = new ServerConfigImpl();
    }

    public static GlobalSettings getInstance() {
        return ApplicationManager.getApplication().getService(GlobalSettings.class);
    }

    /**
     * Produces the state object to persist to file.
     * If configuration loaded from environment-variables, don't persist connection details.
     * Object to persist has null username and password as Password-safe is used for credentials store.
     *
     * @return the state object to persist with clear credentials.
     */
    @Override
    public GlobalSettings getState() {
        ServerConfigImpl serverConfig = new ServerConfigImpl();
        serverConfig.setExcludedPaths(this.serverConfig.getExcludedPaths());
        serverConfig.setPolicyType(this.serverConfig.getPolicyType());
        serverConfig.setProject(this.serverConfig.getProject());
        serverConfig.setWatches(this.serverConfig.getWatches());
        serverConfig.setConnectionRetries(this.serverConfig.getConnectionRetries());
        serverConfig.setConnectionTimeout(this.serverConfig.getConnectionTimeout());
        serverConfig.setExternalResourcesRepo(this.serverConfig.getExternalResourcesRepo());

        GlobalSettings settings = new GlobalSettings();
        settings.serverConfig = serverConfig;
        settings.serverConfig.setPassword(null);
        settings.serverConfig.setUsername(null);
        settings.serverConfig.setUrl(this.serverConfig.getUrl());
        settings.serverConfig.setXrayUrl(this.serverConfig.getXrayUrl());
        settings.serverConfig.setArtifactoryUrl(this.serverConfig.getArtifactoryUrl());
        return settings;
    }

    @Override
    public void loadState(@NotNull GlobalSettings state) {
        XmlSerializerUtil.copyBean(state, this);
        serverConfig.readMissingConfFromEnv();
    }

    @Override
    public void noStateLoaded() {
        reloadMissingConfiguration();
    }

    /**
     * Method is called by Idea IS for reading the previously saved config file 'jfrogConfig.xml' from the disk.
     * Check if previous configurations contain credentials, perform migration if necessary.
     *
     * @param serverConfig - configurations read from file.
     */
    public void setServerConfig(@NotNull ServerConfigImpl serverConfig) {
        setCommonConfigFields(serverConfig);
        this.serverConfig.setCredentials(serverConfig.getCredentialsFromPasswordSafe());
    }

    /**
     * Update xray configurations with new values.
     *
     * @param serverConfig - the new configurations to update.
     */
    public void updateConfig(ServerConfigImpl serverConfig) {
        if (this.serverConfig.getUrl() != null && !this.serverConfig.getUrl().equals(serverConfig.getUrl())) {
            this.serverConfig.removeCredentialsFromPasswordSafe();
        }
        setCommonConfigFields(serverConfig);
        this.serverConfig.setUsername(serverConfig.getUsername());
        this.serverConfig.setPassword(serverConfig.getPassword());
        this.serverConfig.setAccessToken(serverConfig.getAccessToken());
        this.serverConfig.addCredentialsToPasswordSafe();

        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE).update();
    }

    public void setCommonConfigFields(ServerConfigImpl serverConfig) {
        this.serverConfig.setConnectionType(serverConfig.getConnectionType());
        this.serverConfig.setUrl(serverConfig.getUrl());
        this.serverConfig.setXrayUrl(serverConfig.getXrayUrl());
        this.serverConfig.setArtifactoryUrl(serverConfig.getArtifactoryUrl());
        this.serverConfig.setJFrogSettingsCredentialsKey(serverConfig.getJFrogSettingsCredentialsKey());
        setAdvancedSettings(serverConfig);
    }

    private void setAdvancedSettings(ServerConfigImpl serverConfig) {
        this.serverConfig.setExcludedPaths(serverConfig.getExcludedPaths());
        this.serverConfig.setConnectionRetries(serverConfig.getConnectionRetries());
        this.serverConfig.setConnectionTimeout(serverConfig.getConnectionTimeout());
        this.serverConfig.setExternalResourcesRepo(serverConfig.getExternalResourcesRepo());
        this.serverConfig.setPolicyType(serverConfig.getPolicyType());
        this.serverConfig.setProject(serverConfig.getProject());
        this.serverConfig.setWatches(serverConfig.getWatches());
    }

    /**
     * Reloads missing configuration from the plugin settings, environment variables or JFrog CLI configuration.
     *
     * @return true if credentials exist and Xray is configured, false otherwise.
     */
    public boolean reloadMissingConfiguration() {
        serverConfig.readMissingConfFromEnv();
        if (serverConfig.isXrayConfigured()) {
            return true;
        }
        serverConfig.readConnectionDetailsFromEnv();
        if (serverConfig.isXrayConfigured()) {
            return true;
        }
        loadConnectionDetailsFromJfrogCli();
        return serverConfig.isXrayConfigured();
    }

    public boolean areXrayCredentialsSet() {
        return serverConfig != null && serverConfig.isXrayConfigured();
    }

    public boolean areArtifactoryCredentialsSet() {
        return serverConfig != null && serverConfig.isArtifactoryConfigured();
    }

    /**
     * The plugin supports reading the JFrog connection details from JFrog CLI's configuration.
     * This allows developers who already have JFrog CLI installed and configured,
     * to have IDEA load the config automatically.
     */
    private void loadConnectionDetailsFromJfrogCli() {
        try {
            if (serverConfig.readConnectionDetailsFromJfrogCli()) {
                Logger.getInstance().info("Successfully loaded config connection details from JFrog CLI");
                MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
                messageBus.syncPublisher(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE).update();
                return;
            }
        } catch (IOException exception) {
            Logger.getInstance().warn(ExceptionUtils.getRootCauseMessage(exception));
        }
        Logger.getInstance().debug("Couldn't load config connection details from JFrog CLI");
    }
}
