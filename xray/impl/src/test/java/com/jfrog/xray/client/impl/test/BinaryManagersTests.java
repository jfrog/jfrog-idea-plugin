package com.jfrog.xray.client.impl.test;

import com.jfrog.xray.client.services.binarymanagers.ArtifactoryConfiguration;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertNotNull;

/**
 * Created by romang on 2/5/17.
 */
public class BinaryManagersTests extends XrayTestsBase {
    @Test
    public void testGetBinaryManagers() throws IOException {
        List<? extends ArtifactoryConfiguration> artConfigurations = xray.binaryManagers().artifactoryConfigurations();
        for (ArtifactoryConfiguration artConfiguration : artConfigurations) {
            assertNotNull(artConfiguration.getBinMgrUrl());
            assertNotNull(artConfiguration.getBinMgrId());
            assertNotNull(artConfiguration.getVersion());
        }
    }

}
