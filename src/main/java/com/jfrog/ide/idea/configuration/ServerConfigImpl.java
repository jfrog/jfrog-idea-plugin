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

import com.intellij.credentialStore.Credentials;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.jfrog.ide.common.configuration.JfrogCliDriver;
import com.jfrog.ide.common.configuration.JfrogCliServerConfig;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.ui.configuration.ConnectionRetriesSpinner;
import com.jfrog.ide.idea.ui.configuration.ConnectionTimeoutSpinner;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.client.ProxyConfiguration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import static com.jfrog.ide.idea.ui.configuration.ExclusionsVerifier.DEFAULT_EXCLUSIONS;
import static com.jfrog.ide.idea.ui.configuration.Utils.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author yahavi
 */
@Immutable
public class ServerConfigImpl implements ServerConfig {
    private static final String JFROG_SETTINGS_CREDENTIALS_KEY = "credentials";
    static final String ARTIFACTORY_URL_ENV = "JFROG_IDE_ARTIFACTORY_URL";
    public static final String JFROG_SETTINGS_KEY = "com.jfrog.idea";
    static final String PLATFORM_URL_ENV = "JFROG_IDE_PLATFORM_URL";
    static final String XRAY_URL_ENV = "JFROG_IDE_XRAY_URL";
    static final String USERNAME_ENV = "JFROG_IDE_USERNAME";
    static final String PASSWORD_ENV = "JFROG_IDE_PASSWORD";
    static final String ACCESS_TOKEN_ENV = "JFROG_IDE_ACCESS_TOKEN";
    static final String PROJECT_ENV = "JFROG_IDE_PROJECT";

    @Deprecated
    public static final String XRAY_SETTINGS_KEY = "com.jfrog.xray.idea";
    @Deprecated
    static final String LEGACY_XRAY_URL_ENV = "JFROG_IDE_URL";

    @OptionTag
    private String url;
    @OptionTag
    private String xrayUrl;
    @OptionTag
    private String artifactoryUrl;
    @OptionTag
    private String username;
    @Tag
    private String password;
    @Tag
    private String accessToken;
    @Tag
    private PolicyType policyType;
    // JFrog project key to be used as context to Xray scan.
    @OptionTag
    private String project;
    // A comma separated list of Xray watches to be used as context to Xray scan.
    @OptionTag
    private String watches;
    // Pattern of project paths to exclude from Xray scanning for npm
    @Tag
    private String excludedPaths;
    @Tag
    private boolean connectionDetailsFromEnv;
    @Tag
    private Integer connectionRetries;
    @Tag
    private Integer connectionTimeout;
    // The subsystem key of the plugin configuration in the PasswordSafe
    @Transient
    private String jfrogSettingsCredentialsKey = JFROG_SETTINGS_KEY;
    // The subsystem key of the legacy plugin configuration in the PasswordSafe
    @Transient
    @Deprecated
    private String xraySettingsCredentialsKey = XRAY_SETTINGS_KEY;

    ServerConfigImpl() {
    }

    ServerConfigImpl(Builder builder) {
        this.url = builder.url;
        this.xrayUrl = builder.xrayUrl;
        this.artifactoryUrl = builder.artifactoryUrl;
        this.username = builder.username;
        this.password = builder.password;
        this.accessToken = builder.accessToken;
        this.policyType = builder.policyType;
        this.project = builder.project;
        this.watches = builder.watches;
        this.excludedPaths = builder.excludedPaths;
        this.connectionDetailsFromEnv = builder.connectionDetailsFromEnv;
        this.connectionRetries = builder.connectionRetries;
        this.connectionTimeout = builder.connectionTimeout;
        this.jfrogSettingsCredentialsKey = builder.jfrogSettingsCredentialsKey;
        this.xraySettingsCredentialsKey = builder.xraySettingsCredentialsKey;
    }

    public boolean isXrayConfigured() {
        return isNotBlank(xrayUrl) && isAuthenticationConfigured();
    }

    public boolean isArtifactoryConfigured() {
        return isNotBlank(artifactoryUrl) && isAuthenticationConfigured();
    }

    private boolean isAuthenticationConfigured() {
        return isNoneBlank(username, password) || isNotBlank(accessToken);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ServerConfigImpl)) {
            return false;
        }
        ServerConfigImpl other = (ServerConfigImpl) o;

        return Objects.equals(getUrl(), other.getUrl()) &&
                Objects.equals(getXrayUrl(), other.getXrayUrl()) &&
                Objects.equals(getArtifactoryUrl(), other.getArtifactoryUrl()) &&
                Objects.equals(getPassword(), other.getPassword()) &&
                Objects.equals(getUsername(), other.getUsername()) &&
                Objects.equals(getAccessToken(), other.getAccessToken()) &&
                Objects.equals(getPolicyType(), other.getPolicyType()) &&
                Objects.equals(getProject(), other.getProject()) &&
                Objects.equals(getWatches(), other.getWatches()) &&
                Objects.equals(getExcludedPaths(), other.getExcludedPaths()) &&
                isConnectionDetailsFromEnv() == other.isConnectionDetailsFromEnv() &&
                getConnectionRetries() == other.getConnectionRetries() &&
                getConnectionTimeout() == other.getConnectionTimeout();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUrl(), getXrayUrl(), getArtifactoryUrl(), getPassword(), getAccessToken(), getUsername(), getProject(),
                getExcludedPaths(), isConnectionDetailsFromEnv(), getConnectionRetries(), getConnectionTimeout());
    }

    @Override
    public String getUsername() {
        return trimToEmpty(username);
    }

    @Override
    public String getUrl() {
        return trimToEmpty(url);
    }

    @Override
    public String getXrayUrl() {
        return trimToEmpty(xrayUrl);
    }

    @Override
    public String getArtifactoryUrl() {
        return trimToEmpty(artifactoryUrl);
    }

    @Override
    @CheckForNull
    public String getPassword() {
        return password;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    public Credentials getCredentialsFromPasswordSafe() {
        return retrieveCredentialsFromPasswordSafe(jfrogSettingsCredentialsKey, JFROG_SETTINGS_CREDENTIALS_KEY);
    }

    public void addCredentialsToPasswordSafe() {
        String password = isNotBlank(accessToken) ? getAccessToken() : getPassword();
        Credentials credentials = new Credentials(getUsername(), password);
        storeCredentialsInPasswordSafe(jfrogSettingsCredentialsKey, JFROG_SETTINGS_CREDENTIALS_KEY, credentials);
    }

    public void removeCredentialsFromPasswordSafe() {
        removeCredentialsInPasswordSafe(jfrogSettingsCredentialsKey, JFROG_SETTINGS_CREDENTIALS_KEY);
    }

    @Deprecated
    public Credentials getLegacyCredentialsFromPasswordSafe() {
        return retrieveCredentialsFromPasswordSafe(xraySettingsCredentialsKey, getUrl());
    }

    @Deprecated
    public void removeLegacyCredentialsFromPasswordSafe() {
        String url = getUrl();
        if (isNotBlank(url)) {
            removeCredentialsInPasswordSafe(jfrogSettingsCredentialsKey, url);
        }
    }

    @Override
    public boolean isInsecureTls() {
        return CertificateManager.getInstance().getState().ACCEPT_AUTOMATICALLY;
    }

    public String getExcludedPaths() {
        return defaultIfBlank(this.excludedPaths, DEFAULT_EXCLUSIONS);
    }

    @Override
    public PolicyType getPolicyType() {
        return this.policyType;
    }

    @Override
    public String getProject() {
        return trimToEmpty(this.project);
    }

    @Override
    public String getWatches() {
        return trimToEmpty(this.watches);
    }

    @Override
    public SSLContext getSslContext() {
        return CertificateManager.getInstance().getSslContext();
    }

    @Override
    public int getConnectionRetries() {
        return defaultIfNull(this.connectionRetries, ConnectionRetriesSpinner.RANGE.initial);
    }

    @Override
    public int getConnectionTimeout() {
        return defaultIfNull(this.connectionTimeout, ConnectionTimeoutSpinner.RANGE.initial);
    }

    public String getJFrogSettingsCredentialsKey() {
        return this.jfrogSettingsCredentialsKey;
    }

    @Deprecated
    public String getXraySettingsCredentialsKey() {
        return this.xraySettingsCredentialsKey;
    }

    void setExcludedPaths(String excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    void setProject(String project) {
        this.project = project;
    }

    void setWatches(String watches) {
        this.watches = watches;
    }

    /**
     * Get proxy configuration as configured under 'Appearance & Behavior' -> 'System Settings' -> 'HTTP Proxy'
     *
     * @param xrayUrl - The xray URL. The URL is necessary to determine whether to bypass proxy or to pick the relevant
     *                proxy configuration for the Xray URL as configured in *.pac file.
     * @return the proxy configuration as configured in IDEA settings.
     */
    @Override
    public ProxyConfiguration getProxyConfForTargetUrl(String xrayUrl) {
        HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        if (httpConfigurable.USE_PROXY_PAC) {
            // 'Auto-detect proxy settings' option is selected
            return getProxyConfForTargetUrlUsingPac(httpConfigurable, xrayUrl);
        }
        if (httpConfigurable.isHttpProxyEnabledForUrl(xrayUrl)) {
            // 'Manual proxy configuration' option is selected
            return getProxyConfForTargetUrlUsingManualConf(httpConfigurable);
        }
        // 'No proxy' option is selected
        return null;
    }

    /**
     * Read Proxy config from proxy auto-configuration (PAC) file.
     *
     * @param httpConfigurable - Intellij HTTP details
     * @param xrayUrl          - The Xray URL
     * @return Proxy config
     */
    private ProxyConfiguration getProxyConfForTargetUrlUsingPac(HttpConfigurable httpConfigurable, String xrayUrl) {
        URI xrayUri = VfsUtil.toUri(xrayUrl);
        if (xrayUri == null) {
            // Proxy URL is illegal
            return null;
        }

        List<Proxy> proxies = httpConfigurable.getOnlyBySettingsSelector().select(xrayUri);
        if (CollectionUtils.isEmpty(proxies)) {
            // No proxy found for Xray URL
            return null;
        }
        // Currently only 1 proxy is supported
        Proxy firstProxy = proxies.get(0);
        if (firstProxy.type().equals(Proxy.Type.DIRECT)) {
            // Xray URL is configured with "no proxy"
            return null;
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) firstProxy.address();
        ProxyConfiguration proxyConfig = new ProxyConfiguration();
        proxyConfig.host = inetSocketAddress.getHostString();
        proxyConfig.port = inetSocketAddress.getPort();
        if (httpConfigurable.isGenericPasswordCanceled(proxyConfig.host, proxyConfig.port)) {
            // Authentication is disabled
            return proxyConfig;
        }
        PasswordAuthentication passwordAuthentication = httpConfigurable.getGenericPassword(proxyConfig.host, proxyConfig.port);
        if (passwordAuthentication != null) {
            proxyConfig.username = passwordAuthentication.getUserName();
            proxyConfig.password = String.valueOf(passwordAuthentication.getPassword());
        }
        return proxyConfig;
    }

    /**
     * Read Proxy config using manual proxy configuration.
     *
     * @param httpConfigurable - Intellij HTTP details
     * @return Proxy config
     */
    private ProxyConfiguration getProxyConfForTargetUrlUsingManualConf(HttpConfigurable httpConfigurable) {
        ProxyConfiguration proxyConfig = new ProxyConfiguration();
        proxyConfig.host = trimToEmpty(httpConfigurable.PROXY_HOST);
        proxyConfig.port = httpConfigurable.PROXY_PORT;
        if (httpConfigurable.PROXY_AUTHENTICATION) {
            proxyConfig.username = trimToEmpty(httpConfigurable.getProxyLogin());
            proxyConfig.password = httpConfigurable.getPlainProxyPassword();
        }
        return proxyConfig;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setXrayUrl(String xrayUrl) {
        this.xrayUrl = xrayUrl;
    }

    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    void setCredentials(Credentials credentials) {
        if (credentials == null) {
            return;
        }
        if (isNotBlank(credentials.getUserName())) {
            setUsername(credentials.getUserName());
            setPassword(credentials.getPasswordAsString());
        } else {
            setAccessToken(credentials.getPasswordAsString());
        }
    }

    void setConnectionDetailsFromEnv(boolean connectionDetailsFromEnv) {
        this.connectionDetailsFromEnv = connectionDetailsFromEnv;
    }

    public boolean isConnectionDetailsFromEnv() {
        return connectionDetailsFromEnv;
    }

    void setConnectionRetries(int connectionRetries) {
        this.connectionRetries = connectionRetries;
    }

    void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setJFrogSettingsCredentialsKey(String jfrogSettingsCredentialsKey) {
        this.jfrogSettingsCredentialsKey = jfrogSettingsCredentialsKey;
    }

    @Deprecated
    public void setXraySettingsCredentialsKey(String xraySettingsCredentialsKey) {
        this.xraySettingsCredentialsKey = xraySettingsCredentialsKey;
    }

    /**
     * Read connection details from environment variables.
     * All connection details must be provided from env, otherwise don't use them.
     *
     * @return true if connection details loaded from env.
     */
    public boolean readConnectionDetailsFromEnv() {
        String platformUrlEnv = EnvironmentUtil.getValue(PLATFORM_URL_ENV);
        String xrayUrlEnv = EnvironmentUtil.getValue(XRAY_URL_ENV);
        String artifactoryUrlEnv = EnvironmentUtil.getValue(ARTIFACTORY_URL_ENV);
        String legacyXrayUrlEnv = EnvironmentUtil.getValue(LEGACY_XRAY_URL_ENV);
        String usernameEnv = EnvironmentUtil.getValue(USERNAME_ENV);
        String passwordEnv = EnvironmentUtil.getValue(PASSWORD_ENV);
        String accessTokenEnv = EnvironmentUtil.getValue(ACCESS_TOKEN_ENV);
        String projectEnv = EnvironmentUtil.getValue(PROJECT_ENV);
        if (isAnyBlank(usernameEnv, passwordEnv) || isAllBlank(platformUrlEnv, xrayUrlEnv, artifactoryUrlEnv, legacyXrayUrlEnv)) {
            setUrl("");
            setXrayUrl("");
            setArtifactoryUrl("");
            setUsername("");
            setPassword("");
            setAccessToken("");
            return false;
        }
        if (isAllBlank(platformUrlEnv, xrayUrlEnv, artifactoryUrlEnv)) {
            setXrayUrl(legacyXrayUrlEnv);
            migrateXrayConfigToPlatformConfig(this);
        } else {
            setUrl(platformUrlEnv);
            setXrayUrl(xrayUrlEnv);
            setArtifactoryUrl(artifactoryUrlEnv);
        }
        if (isNotBlank(projectEnv)) {
            setProject(projectEnv);
        }
        if (isNotBlank(accessTokenEnv)) {
            setAccessToken(accessTokenEnv);
        } else {
            setUsername(usernameEnv);
            setPassword(passwordEnv);
        }
        return true;
    }

    /**
     * Read the connection details from JFrog CLI's config. The configuration is read by executing JFrog CLI.
     * If no JFrog CLI server configuration was found or the config
     * file is encrypt, do nothing.
     *
     * @return true if connection details loaded from JFrog CLI default server.
     */
    public boolean readConnectionDetailsFromJfrogCli() throws IOException {
        JfrogCliDriver driver = new JfrogCliDriver(EnvironmentUtil.getEnvironmentMap());
        if (!driver.isJfrogCliInstalled()) {
            return false;
        }
        JfrogCliServerConfig cliServerConfig = driver.getServerConfig();
        String platformUrlCLi = cliServerConfig.getUrl();
        String xrayUrlCli = cliServerConfig.getXrayUrl();
        String artifactoryUrlCli = cliServerConfig.getArtifactoryUrl();
        String usernameCli = cliServerConfig.getUsername();
        String passwordCli = cliServerConfig.getPassword();
        String accessToken = cliServerConfig.getAccessToken();

        if ((isAnyBlank(usernameCli, passwordCli) && isBlank(accessToken)) || isAnyBlank(xrayUrlCli, artifactoryUrlCli)) {
            return false;
        }
        setUrl(platformUrlCLi);
        setXrayUrl(xrayUrlCli);
        setArtifactoryUrl(artifactoryUrlCli);
        setUsername(usernameCli);
        setPassword(passwordCli);
        setAccessToken(accessToken);
        return true;
    }

    @Override
    public String toString() {
        return url;
    }

    public static class Builder {
        private String xraySettingsCredentialsKey = XRAY_SETTINGS_KEY;
        private String jfrogSettingsCredentialsKey = JFROG_SETTINGS_KEY;
        private String url;
        private String xrayUrl;
        private String artifactoryUrl;
        private String username;
        private String password;
        private String accessToken;
        private String excludedPaths;
        private PolicyType policyType;
        private String project;
        private String watches;
        private boolean connectionDetailsFromEnv;
        private int connectionRetries;
        private int connectionTimeout;

        public ServerConfigImpl build() {
            return new ServerConfigImpl(this);
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setXrayUrl(String xrayUrl) {
            this.xrayUrl = xrayUrl;
            return this;
        }

        public Builder setArtifactoryUrl(String artifactoryUrl) {
            this.artifactoryUrl = artifactoryUrl;
            return this;
        }

        public Builder setUsername(@Nullable String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(@Nullable String password) {
            this.password = defaultString(password);
            return this;
        }

        public Builder setAccessToken(@Nullable String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder setExcludedPaths(@Nullable String excludedPaths) {
            this.excludedPaths = excludedPaths;
            return this;
        }

        public Builder setPolicyType(PolicyType policyType) {
            this.policyType = policyType;
            return this;
        }

        public Builder setProject(@Nullable String project) {
            this.project = project;
            return this;
        }

        public Builder setWatches(@Nullable String watches) {
            this.watches = watches;
            return this;
        }

        public Builder setConnectionDetailsFromEnv(boolean connectionDetailsFromEnv) {
            this.connectionDetailsFromEnv = connectionDetailsFromEnv;
            return this;
        }

        public Builder setConnectionRetries(int connectionRetries) {
            this.connectionRetries = connectionRetries;
            return this;
        }

        public Builder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder setJFrogSettingsCredentialsKey(String jfrogSettingsCredentialsKey) {
            this.jfrogSettingsCredentialsKey = jfrogSettingsCredentialsKey;
            return this;
        }

        public Builder setXraySettingsCredentialsKey(String xraySettingsCredentialsKey) {
            this.xraySettingsCredentialsKey = xraySettingsCredentialsKey;
            return this;
        }
    }
}
