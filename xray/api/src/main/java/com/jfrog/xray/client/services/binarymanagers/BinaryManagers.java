package com.jfrog.xray.client.services.binarymanagers;

import java.io.IOException;
import java.util.List;

/**
 * Created by romang on 2/5/17.
 */
public interface BinaryManagers {

    List<? extends ArtifactoryConfiguration> artifactoryConfigurations() throws IOException;
}
