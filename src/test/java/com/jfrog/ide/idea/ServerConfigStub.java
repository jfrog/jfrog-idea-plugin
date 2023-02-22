package com.jfrog.ide.idea;

import com.jfrog.ide.common.configuration.ServerConfig;
import org.jfrog.build.client.ProxyConfiguration;

import javax.net.ssl.SSLContext;

public class ServerConfigStub implements ServerConfig {
    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public String getXrayUrl() {
        return "";
    }

    @Override
    public String getArtifactoryUrl() {
        return "";
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getAccessToken() {
        return "";
    }

    @Override
    public PolicyType getPolicyType() {
        return null;
    }

    @Override
    public String getProject() {
        return "";
    }

    @Override
    public String getWatches() {
        return "";
    }

    @Override
    public boolean isInsecureTls() {
        return false;
    }

    @Override
    public SSLContext getSslContext() {
        return null;
    }

    @Override
    public ProxyConfiguration getProxyConfForTargetUrl(String xrayUrl) {
        return null;
    }

    @Override
    public int getConnectionRetries() {
        return 0;
    }

    @Override
    public int getConnectionTimeout() {
        return 0;
    }
}
