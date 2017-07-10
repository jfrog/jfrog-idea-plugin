package com.jfrog.xray.client.impl.services.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.xray.client.impl.XrayImpl;
import com.jfrog.xray.client.impl.util.ObjectMapperHelper;
import com.jfrog.xray.client.services.system.System;
import com.jfrog.xray.client.services.system.Version;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;

/**
 * Created by romang on 2/2/17.
 */
public class SystemImpl implements System {

    private XrayImpl xray;

    public SystemImpl(XrayImpl xray) {
        this.xray = xray;
    }

    @Override
    public boolean ping() {
        try {
            HttpResponse response = xray.get("system/ping", null);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return true;
            }
        } catch (Exception e) {
            // Do nothing
        }
        return false;
    }

    @Override
    public Version version() throws IOException {
        ObjectMapper mapper = ObjectMapperHelper.get();

        HttpResponse response = xray.get("system/version", null);
        return mapper.readValue(response.getEntity().getContent(), VersionImpl.class);
    }
}
