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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import static com.jfrog.ide.idea.ui.configuration.Utils.migrateXrayConfigToPlatformConfig;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yahavi
 */
@State(name = "GlobalSettings", storages = {@Storage("jfrogConfig.xml")})
public final class GlobalSettings implements PersistentStateComponent<GlobalSettings> {

    private ServerConfigImpl serverConfig;

    @Deprecated
    private final XrayServerConfigImpl xrayConfig;

    @SuppressWarnings("unused")
    GlobalSettings() {
        this.serverConfig = new ServerConfigImpl();
        this.xrayConfig = new XrayServerConfigImpl();
    }

    public static GlobalSettings getInstance() {
        return ApplicationManager.getApplication().getComponent(GlobalSettings.class);
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
        serverConfig.setConnectionDetailsFromEnv(this.serverConfig.isConnectionDetailsFromEnv());
        serverConfig.setConnectionRetries(this.serverConfig.getConnectionRetries());
        serverConfig.setConnectionTimeout(this.serverConfig.getConnectionTimeout());
        GlobalSettings settings = new GlobalSettings();
        settings.serverConfig = serverConfig;
        if (this.serverConfig.isConnectionDetailsFromEnv()) {
            return settings;
        }

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
    }

    @Override
    public void noStateLoaded() {
        this.serverConfig.setConnectionDetailsFromEnv(this.serverConfig.readConnectionDetailsFromEnv());
    }

    public ServerConfigImpl getServerConfig() {
        return this.serverConfig;
    }

    @Deprecated
    public XrayServerConfigImpl getXrayConfig() {
        return this.xrayConfig;
    }

    /**
     * Method is called by Idea IS for reading the previously saved config file 'jfrogConfig.xml' from the disk.
     * Check if previous configurations contain credentials, perform migration if necessary.
     * If connection details loaded from environment, don't override them.
     *
     * @param xrayConfig - configurations read from file.
     */
    @Deprecated
    public void setXrayConfig(XrayServerConfigImpl xrayConfig) {
        String xrayUrl = xrayConfig.getUrl();
        if (isBlank(xrayUrl)) {
            return;
        }
        xrayConfig.setXrayUrl(xrayUrl);
        migrateXrayConfigToPlatformConfig(xrayConfig);
        setServerConfig(xrayConfig);
    }

    /**
     * Method is called by Idea IS for reading the previously saved config file 'jfrogConfig.xml' from the disk.
     * Check if previous configurations contain credentials, perform migration if necessary.
     * If connection details loaded from environment, don't override them.
     *
     * @param serverConfig - configurations read from file.
     */
    public void setServerConfig(@NotNull ServerConfigImpl serverConfig) {
        if (serverConfig.isConnectionDetailsFromEnv()) {
            // Load connection details from environment variables.
            this.serverConfig.setConnectionDetailsFromEnv(this.serverConfig.readConnectionDetailsFromEnv());
            this.serverConfig.setExcludedPaths(serverConfig.getExcludedPaths());
            this.serverConfig.setConnectionRetries(serverConfig.getConnectionRetries());
            this.serverConfig.setConnectionTimeout(serverConfig.getConnectionTimeout());
            return;
        }

        // Load configuration from state.
        setCommonConfigFields(serverConfig);
        if (shouldPerformCredentialsMigration(serverConfig)) {
            migrateCredentialsFromFileToPasswordSafe(serverConfig);
        } else {
            this.serverConfig.setCredentials(serverConfig.getCredentialsFromPasswordSafe());
        }
    }

    /**
     * Update xray configurations with new values.
     *
     * @param serverConfig - the new configurations to update.
     */
    public void updateConfig(ServerConfigImpl serverConfig) {
        if (serverConfig.isConnectionDetailsFromEnv()) {
            if (this.serverConfig.getUrl() != null) {
                this.serverConfig.removeCredentialsFromPasswordSafe();
            }
            this.serverConfig.setConnectionDetailsFromEnv(true);
            this.serverConfig.readConnectionDetailsFromEnv();
            this.serverConfig.setExcludedPaths(serverConfig.getExcludedPaths());
            this.serverConfig.setConnectionRetries(serverConfig.getConnectionRetries());
            this.serverConfig.setConnectionTimeout(serverConfig.getConnectionTimeout());
            return;
        }

        if (this.serverConfig.getUrl() != null && !this.serverConfig.getUrl().equals(serverConfig.getUrl())) {
            this.serverConfig.removeCredentialsFromPasswordSafe();
        }
        setCommonConfigFields(serverConfig);
        this.serverConfig.setUsername(serverConfig.getUsername());
        this.serverConfig.setPassword(serverConfig.getPassword());
        this.serverConfig.addCredentialsToPasswordSafe();
    }

    public void setCommonConfigFields(ServerConfigImpl serverConfig) {
        this.serverConfig.setUrl(serverConfig.getUrl());
        this.serverConfig.setXrayUrl(serverConfig.getXrayUrl());
        this.serverConfig.setArtifactoryUrl(serverConfig.getArtifactoryUrl());
        this.serverConfig.setExcludedPaths(serverConfig.getExcludedPaths());
        this.serverConfig.setConnectionDetailsFromEnv(serverConfig.isConnectionDetailsFromEnv());
        this.serverConfig.setConnectionRetries(serverConfig.getConnectionRetries());
        this.serverConfig.setConnectionTimeout(serverConfig.getConnectionTimeout());
        this.serverConfig.setJFrogSettingsCredentialsKey(serverConfig.getJFrogSettingsCredentialsKey());
        this.serverConfig.setXraySettingsCredentialsKey(serverConfig.getXraySettingsCredentialsKey());
    }

    public boolean areXrayCredentialsSet() {
        return serverConfig != null && serverConfig.isXrayConfigured();
    }

    public boolean areArtifactoryCredentialsSet() {
        return serverConfig != null && serverConfig.isArtifactoryConfigured();
    }

    /**
     * Perform credentials migration from file to PasswordSafe.
     * If credentials were stored on file, set the new values from it, save to PasswordSafe
     * and persist configuration to file.
     *
     * @param serverConfig - configurations read from 'jfrogConfig.xml'.
     */
    private void migrateCredentialsFromFileToPasswordSafe(ServerConfigImpl serverConfig) {
        this.serverConfig.setUsername(serverConfig.getUsername());
        this.serverConfig.setPassword(serverConfig.getPassword());
        this.serverConfig.addCredentialsToPasswordSafe();
        Application application = ApplicationManager.getApplication();
        application.invokeLater(application::saveSettings);
    }

    /**
     * Determine whether should perform credentials migration or not.
     *
     * @param xrayConfig - configurations read from 'jfrogConfig.xml'.
     * @return true if credentials are stored in file.
     */
    private boolean shouldPerformCredentialsMigration(ServerConfigImpl xrayConfig) {
        return !isAnyBlank(xrayConfig.getUsername(), xrayConfig.getPassword());
    }
}
