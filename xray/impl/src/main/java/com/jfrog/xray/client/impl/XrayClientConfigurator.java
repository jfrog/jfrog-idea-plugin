package com.jfrog.xray.client.impl;

import org.jfrog.client.http.HttpClientConfiguratorBase;
import org.jfrog.client.http.auth.PreemptiveAuthInterceptor;

/**
 * Created by romang on 1/31/17.
 */
public class XrayClientConfigurator extends HttpClientConfiguratorBase {
    @Override
    protected void additionalConfigByAuthScheme() {
        // Preemptive authorization interceptor
        builder.addInterceptorFirst(new PreemptiveAuthInterceptor());
    }

    public void setUserAgent(String userAgent) {
        builder.setUserAgent(userAgent);
    }
}