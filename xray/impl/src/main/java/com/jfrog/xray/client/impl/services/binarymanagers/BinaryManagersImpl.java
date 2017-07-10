package com.jfrog.xray.client.impl.services.binarymanagers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.xray.client.impl.XrayImpl;
import com.jfrog.xray.client.impl.util.ObjectMapperHelper;
import com.jfrog.xray.client.services.binarymanagers.BinaryManagers;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.List;

/**
 * Created by romang on 2/5/17.
 */
public class BinaryManagersImpl implements BinaryManagers {
    private final XrayImpl xray;

    public BinaryManagersImpl(XrayImpl xray) {
        this.xray = xray;
    }

    public List<ArtifactoryConfigurationImpl> artifactoryConfigurations() throws IOException {
        ObjectMapper mapper = ObjectMapperHelper.get();
        HttpResponse response = xray.get("binMgr", null);
        return mapper.readValue(response.getEntity().getContent(), new TypeReference<List<ArtifactoryConfigurationImpl>>() {
        });
    }
}
