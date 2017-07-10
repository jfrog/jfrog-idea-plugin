package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.summary.Artifact;
import com.jfrog.xray.client.services.summary.General;
import com.jfrog.xray.client.services.summary.Issue;
import com.jfrog.xray.client.services.summary.License;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactImpl implements Artifact {

    private GeneralImpl general;
    private List<IssueImpl> issues = null;
    private List<LicenseImpl> licenses = null;

    public ArtifactImpl() {
    }

    @JsonProperty("general")
    public General getGeneral() {
        return general;
    }

    @JsonProperty("issues")
    public List<? extends Issue> getIssues() {
        return issues;
    }

    @JsonProperty("licenses")
    public List<? extends License> getLicenses() {
        return licenses;
    }
}