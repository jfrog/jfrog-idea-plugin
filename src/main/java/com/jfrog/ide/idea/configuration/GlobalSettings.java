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
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

/**
 * @author yahavi
 */
@State(name = "GlobalSettings", storages = {@Storage("jfrogConfig.xml")})
public final class GlobalSettings implements ApplicationComponent, PersistentStateComponent<GlobalSettings> {

    private XrayServerConfigImpl xrayConfig = new XrayServerConfigImpl();

    public static GlobalSettings getInstance() {
        return ApplicationManager.getApplication().getComponent(GlobalSettings.class);
    }

    @Override
    public GlobalSettings getState() {
        return this;
    }

    @Override
    public void loadState(GlobalSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public XrayServerConfigImpl getXrayConfig() {
        return this.xrayConfig;
    }

    /**
     * Method is called by Idea IS for reading the previously saved config file 'jfrogConfig.xml' from the disk.
     * @param xrayConfig
     */
    @SuppressWarnings("unused")
    public void setXrayConfig(XrayServerConfigImpl xrayConfig) {
        setCommonConfigFields(xrayConfig);
        this.xrayConfig.setCredentials(xrayConfig.getCredentialsFromPasswordSafe());
    }

    /**
     * Update xray configurations with new values.
     * @param xrayConfig - the new configurations to update.
     */
    public void updateConfig(XrayServerConfigImpl xrayConfig) {
        if (this.xrayConfig.getUrl() != null && !this.xrayConfig.getUrl().equals(xrayConfig.getUrl())) {
            this.xrayConfig.removeCredentialsFromPasswordSafe();
        }
        setCommonConfigFields(xrayConfig);
        this.xrayConfig.setPassword(xrayConfig.getPassword());
        this.xrayConfig.addCredentialsToPasswordSafe();
    }

    public void setCommonConfigFields(XrayServerConfigImpl xrayConfig) {
        this.xrayConfig.setUrl(xrayConfig.getUrl());
        this.xrayConfig.setUsername(xrayConfig.getUsername());
        this.xrayConfig.setExcludedPaths(xrayConfig.getExcludedPaths());
    }

    public boolean areCredentialsSet() {
        return xrayConfig != null && !xrayConfig.isEmpty();
    }
}
