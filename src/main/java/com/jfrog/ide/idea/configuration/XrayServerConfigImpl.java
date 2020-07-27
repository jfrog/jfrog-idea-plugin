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

import com.google.common.base.Objects;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Tag;
import com.jfrog.ide.common.configuration.XrayServerConfig;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.client.http.model.ProxyConfig;
import org.jfrog.client.util.KeyStoreProvider;
import org.jfrog.client.util.KeyStoreProviderException;
import org.jfrog.client.util.KeyStoreProviderFactory;
import org.springframework.util.CollectionUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * @author yahavi
 */
@Immutable
public class XrayServerConfigImpl implements XrayServerConfig {
    private static final String XRAY_SETTINGS_CREDENTIALS_KEY = "com.jfrog.xray.idea";
    public static final String DEFAULT_EXCLUSIONS = "**/*{.idea,test,node_modules}*";
    private static final String USERNAME_ENV = "JFROG_IDE_USERNAME";
    private static final String PASSWORD_ENV = "JFROG_IDE_PASSWORD";
    private static final String URL_ENV = "JFROG_IDE_URL";

    @OptionTag
    private String url;
    @OptionTag
    private String username;
    @Tag
    private String password;
    @Tag
    private String excludedPaths; // Pattern of project paths to exclude from Xray scanning for npm
    @Tag
    private boolean connectionDetailsFromEnv;

    XrayServerConfigImpl() {
    }

    private XrayServerConfigImpl(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
        this.excludedPaths = builder.excludedPaths;
        this.connectionDetailsFromEnv = builder.connectionDetailsFromEnv;
    }

    boolean isEmpty() {
        return StringUtils.isEmpty(this.url) && StringUtils.isEmpty(this.username) && StringUtils.isEmpty(this.password);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof XrayServerConfigImpl)) {
            return false;
        }
        XrayServerConfigImpl other = (XrayServerConfigImpl) o;

        return Comparing.equal(getUrl(), other.getUrl()) &&
                Comparing.equal(getPassword(), other.getPassword()) &&
                Comparing.equal(getUsername(), other.getUsername()) &&
                Comparing.equal(getExcludedPaths(), other.getExcludedPaths()) &&
                Comparing.equal(isConnectionDetailsFromEnv(), other.isConnectionDetailsFromEnv());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUrl(), getPassword(), getUsername(), isConnectionDetailsFromEnv());
    }

    @Override
    @CheckForNull
    public String getUsername() {
        return trim(username);
    }

    @Override
    public String getUrl() {
        return trim(url);
    }

    @Override
    @CheckForNull
    public String getPassword() {
        return password;
    }

    public Credentials getCredentialsFromPasswordSafe() {
        if (StringUtils.isEmpty(getUrl())) {
            return null;
        }
        try {
            return PasswordSafe.getInstance().get(getCredentialAttributes());
        } catch (Exception e) {
            return null;
        }
    }

    public void addCredentialsToPasswordSafe() {
        if (StringUtils.isEmpty(getUrl())) {
            return;
        }
        Credentials credentials = new Credentials(getUsername(), getPassword());
        PasswordSafe.getInstance().set(getCredentialAttributes(), credentials);
    }

    public void removeCredentialsFromPasswordSafe() {
        PasswordSafe.getInstance().set(getCredentialAttributes(), null);
    }

    public CredentialAttributes getCredentialAttributes() {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(XRAY_SETTINGS_CREDENTIALS_KEY, getUrl()));
    }

    @Override
    public boolean isNoHostVerification() {
        return CertificateManager.getInstance().getState().ACCEPT_AUTOMATICALLY;
    }

    public String getExcludedPaths() {
        return StringUtils.defaultIfBlank(this.excludedPaths, DEFAULT_EXCLUSIONS);
    }

    @Override
    public KeyStoreProvider getKeyStoreProvider() throws KeyStoreProviderException {
        CertificateManager certificateManager = CertificateManager.getInstance();
        if (ArrayUtils.isEmpty(certificateManager.getCustomTrustManager().getAcceptedIssuers())) {
            return null;
        }
        return KeyStoreProviderFactory.getProvider(certificateManager.getCacertsPath(), certificateManager.getPassword());
    }

    void setExcludedPaths(String excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    @Override
    public ProxyConfig getProxyConfForTargetUrl(String xrayUrl) {
        HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        if (httpConfigurable.USE_PROXY_PAC) {
            return getProxyConfForTargetUrlUsingPac(httpConfigurable, xrayUrl);
        }
        if (httpConfigurable.isHttpProxyEnabledForUrl(xrayUrl)) {
            return getProxyConfForTargetUrlUsingManualConf(httpConfigurable);
        }
        return null;
    }

    /**
     * Read Proxy config from proxy auto-configuration (PAC) file.
     *
     * @param httpConfigurable - Intellij HTTP details
     * @param xrayUrl          - The Xray URL
     * @return Proxy config
     */
    private ProxyConfig getProxyConfForTargetUrlUsingPac(HttpConfigurable httpConfigurable, String xrayUrl) {
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
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHost(inetSocketAddress.getHostString());
        proxyConfig.setPort(inetSocketAddress.getPort());
        if (httpConfigurable.isGenericPasswordCanceled(proxyConfig.getHost(), proxyConfig.getPort())) {
            // Authentication is disabled
            return proxyConfig;
        }
        PasswordAuthentication passwordAuthentication = httpConfigurable.getGenericPassword(proxyConfig.getHost(), proxyConfig.getPort());
        if (passwordAuthentication != null) {
            proxyConfig.setUsername(passwordAuthentication.getUserName());
            proxyConfig.setPassword(String.valueOf(passwordAuthentication.getPassword()));
        }
        return proxyConfig;
    }

    /**
     * Read Proxy config using manual proxy configuration.
     *
     * @param httpConfigurable - Intellij HTTP details
     * @return Proxy config
     */
    private ProxyConfig getProxyConfForTargetUrlUsingManualConf(HttpConfigurable httpConfigurable) {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHost(trim(httpConfigurable.PROXY_HOST));
        proxyConfig.setPort(httpConfigurable.PROXY_PORT);
        if (httpConfigurable.PROXY_AUTHENTICATION) {
            proxyConfig.setUsername(trim(httpConfigurable.getProxyLogin()));
            proxyConfig.setPassword(httpConfigurable.getPlainProxyPassword());
        }
        return proxyConfig;
    }

    void setUrl(String url) {
        this.url = url;
    }

    void setUsername(String username) {
        this.username = username;
    }

    void setPassword(String password) {
        this.password = password;
    }

    void setCredentials(Credentials credentials) {
        if (credentials == null) {
            return;
        }
        setUsername(credentials.getUserName());
        setPassword(credentials.getPasswordAsString());
    }

    void setConnectionDetailsFromEnv(boolean connectionDetailsFromEnv) {
        this.connectionDetailsFromEnv = connectionDetailsFromEnv;
    }

    public boolean isConnectionDetailsFromEnv() {
        return connectionDetailsFromEnv;
    }

    /**
     * Read connection details from environment variables.
     * All connection details must be provided from env, otherwise don't use them.
     *
     * @return true if connection details loaded from env.
     */
    public boolean readConnectionDetailsFromEnv() {
        String urlEnv = EnvironmentUtil.getValue(URL_ENV);
        String usernameEnv = EnvironmentUtil.getValue(USERNAME_ENV);
        String passwordEnv = EnvironmentUtil.getValue(PASSWORD_ENV);
        if (StringUtils.isBlank(urlEnv) || StringUtils.isBlank(usernameEnv) || StringUtils.isBlank(passwordEnv)) {
            setUrl("");
            setUsername("");
            setPassword("");
            return false;
        }
        setUrl(urlEnv);
        setUsername(usernameEnv);
        setPassword(passwordEnv);
        return true;
    }

    @Override
    public String toString() {
        return url;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String username;
        private String password;
        private String excludedPaths;
        private boolean connectionDetailsFromEnv;

        private Builder() {
            // no args
        }

        public XrayServerConfigImpl build() {
            return new XrayServerConfigImpl(this);
        }

        public Builder setUsername(@Nullable String username) {
            this.username = username;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setPassword(@Nullable String password) {
            this.password = password != null ? password : "";
            return this;
        }

        public Builder setExcludedPaths(@Nullable String excludedPaths) {
            this.excludedPaths = excludedPaths;
            return this;
        }

        public Builder setConnectionDetailsFromEnv(boolean connectionDetailsFromEnv) {
            this.connectionDetailsFromEnv = connectionDetailsFromEnv;
            return this;
        }
    }
}
