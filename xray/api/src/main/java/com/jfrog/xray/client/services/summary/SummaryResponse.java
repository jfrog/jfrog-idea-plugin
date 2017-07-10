package com.jfrog.xray.client.services.summary;

import java.io.Serializable;
import java.util.List;

/**
 * Created by romang on 2/27/17.
 */
public interface SummaryResponse extends Serializable {

    List<? extends Artifact> getArtifacts();

    List<? extends Error> getErrors();

}
