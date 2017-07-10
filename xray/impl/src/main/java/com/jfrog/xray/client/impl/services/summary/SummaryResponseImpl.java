package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.summary.Artifact;
import com.jfrog.xray.client.services.summary.Error;
import com.jfrog.xray.client.services.summary.SummaryResponse;

import java.util.List;

/**
 * Created by romang on 2/27/17.
 */
public class SummaryResponseImpl implements SummaryResponse {

    private List<ArtifactImpl> artifacts = null;
    private List<ErrorImpl> errors = null;

    @JsonProperty("artifacts")
    public List<? extends Artifact> getArtifacts() {
        return artifacts;
    }

    @JsonProperty("errors")
    public List<? extends Error> getErrors() {
        return errors;
    }
}
