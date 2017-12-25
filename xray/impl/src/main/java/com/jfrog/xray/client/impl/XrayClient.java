package com.jfrog.xray.client.impl;

import com.jfrog.xray.client.Xray;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Created by romang on 2/2/17.
 */
public class XrayClient {

    private static final int CONNECTION_TIMEOUT_MILLISECONDS = 300 * 1000;
    private static final String DEFAULT_USER_AGENT = "jfrog-xray-client/" + XrayClient.class.getPackage().getImplementationVersion();

    static public Xray create(CloseableHttpClient preConfiguredClient, String url) {
        return new XrayImpl(preConfiguredClient, url);
    }

    /**
     * Username, API key, and custom url
     */
    static public Xray create(String url, String username, String password, String userAgent) {
        XrayClientConfigurator configurator = new XrayClientConfigurator();
        configurator.setHostFromUrl(url);
        configurator.setCredentials(username, password, true);
        configurator.setConnectTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        configurator.setSocketTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        configurator.setUserAgent(userAgent);

        return new XrayImpl(configurator.getClient(), url);
    }

    static public Xray create(String url, String username, String password) {
        return create(url, username, password, DEFAULT_USER_AGENT);
    }
}