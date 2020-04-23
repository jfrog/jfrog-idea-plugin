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
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author yahavi
 */
@State(name = "GlobalSettings", storages = {@Storage("jfrogConfig.xml")})
public final class GlobalSettings implements PersistentStateComponent<GlobalSettings> {

    private XrayServerConfigImpl xrayConfig;

    @SuppressWarnings("unused")
    GlobalSettings() {
        this.xrayConfig = new XrayServerConfigImpl();
    }

    GlobalSettings(XrayServerConfigImpl xrayConfig) {
        this.xrayConfig = xrayConfig;
    }

    public static GlobalSettings getInstance() {
        return ApplicationManager.getApplication().getComponent(GlobalSettings.class);
    }

    /**
     * Produces the state object to persist to file.
     * If configuration loaded from environment-variables, don't persist connection details.
     * Object to persist has null username and password as Password-safe is used for credentials store.
     * @return the state object to persist with clear credentials.
     */
    @Override
    public GlobalSettings getState() {
        XrayServerConfigImpl serverConfig = new XrayServerConfigImpl();
        serverConfig.setExcludedPaths(this.xrayConfig.getExcludedPaths());
        serverConfig.setConnectionDetailsFromEnv(this.xrayConfig.isConnectionDetailsFromEnv());
        GlobalSettings settings = new GlobalSettings(serverConfig);
        if (this.xrayConfig.isConnectionDetailsFromEnv()) {
            return settings;
        }

        settings.xrayConfig.setPassword(null);
        settings.xrayConfig.setUsername(null);
        settings.xrayConfig.setUrl(this.xrayConfig.getUrl());
        return settings;
    }

    @Override
    public void loadState(@NotNull GlobalSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public void noStateLoaded() {
        this.xrayConfig.setConnectionDetailsFromEnv(this.xrayConfig.readConnectionDetailsFromEnv());
    }

    public XrayServerConfigImpl getXrayConfig() {
        return this.xrayConfig;
    }

    /**
     * Method is called by Idea IS for reading the previously saved config file 'jfrogConfig.xml' from the disk.
     * Check if previous configurations contain credentials, perform migration if necessary.
     * If connection details loaded from environment, don't override them.
     * @param xrayConfig - configurations read from file.
     */
    @SuppressWarnings("unused")
    public void setXrayConfig(@NotNull XrayServerConfigImpl xrayConfig) {
        if (xrayConfig.isConnectionDetailsFromEnv()) {
            // Load connection details from environment variables.
            this.xrayConfig.setConnectionDetailsFromEnv(this.xrayConfig.readConnectionDetailsFromEnv());
            this.xrayConfig.setExcludedPaths(xrayConfig.getExcludedPaths());
            return;
        }

        // Load configuration from state.
        setCommonConfigFields(xrayConfig);
        if (shouldPerformCredentialsMigration(xrayConfig)) {
            migrateCredentialsFromFileToPasswordSafe(xrayConfig);
        } else {
            this.xrayConfig.setCredentials(xrayConfig.getCredentialsFromPasswordSafe());
        }
    }

    /**
     * Update xray configurations with new values.
     * @param xrayConfig - the new configurations to update.
     */
    public void updateConfig(XrayServerConfigImpl xrayConfig) {
        if (xrayConfig.isConnectionDetailsFromEnv()) {
            if (this.xrayConfig.getUrl() != null) {
                this.xrayConfig.removeCredentialsFromPasswordSafe();
            }
            this.xrayConfig.setConnectionDetailsFromEnv(true);
            this.xrayConfig.readConnectionDetailsFromEnv();
            this.xrayConfig.setExcludedPaths(xrayConfig.getExcludedPaths());
            return;
        }

        if (this.xrayConfig.getUrl() != null && !this.xrayConfig.getUrl().equals(xrayConfig.getUrl())) {
            this.xrayConfig.removeCredentialsFromPasswordSafe();
        }
        setCommonConfigFields(xrayConfig);
        this.xrayConfig.setUsername(xrayConfig.getUsername());
        this.xrayConfig.setPassword(xrayConfig.getPassword());
        this.xrayConfig.addCredentialsToPasswordSafe();
    }

    public void setCommonConfigFields(XrayServerConfigImpl xrayConfig) {
        this.xrayConfig.setUrl(xrayConfig.getUrl());
        this.xrayConfig.setExcludedPaths(xrayConfig.getExcludedPaths());
        this.xrayConfig.setConnectionDetailsFromEnv(xrayConfig.isConnectionDetailsFromEnv());
    }

    public boolean areCredentialsSet() {
        return xrayConfig != null && !xrayConfig.isEmpty();
    }

    /**
     * Perform credentials migration from file to PasswordSafe.
     * If credentials were stored on file, set the new values from it, save to PasswordSafe
     * and persist configuration to file.
     * @param xrayConfig - configurations read from 'jfrogConfig.xml'.
     */
    private void migrateCredentialsFromFileToPasswordSafe(XrayServerConfigImpl xrayConfig) {
        this.xrayConfig.setUsername(xrayConfig.getUsername());
        this.xrayConfig.setPassword(xrayConfig.getPassword());
        this.xrayConfig.addCredentialsToPasswordSafe();
        ApplicationManager.getApplication().saveSettings();
    }

    /**
     * Determine whether should perform credentials migration or not.
     * @param xrayConfig - configurations read from 'jfrogConfig.xml'.
     * @return true if credentials are stored in file.
     */
    private boolean shouldPerformCredentialsMigration(XrayServerConfigImpl xrayConfig) {
        return !StringUtils.isBlank(xrayConfig.getUsername()) || !StringUtils.isBlank(xrayConfig.getPassword());
    }
}
