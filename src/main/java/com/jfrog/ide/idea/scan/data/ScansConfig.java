package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "scans",
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScansConfig {
    @JsonProperty("scans")
    private List<ScanConfig> scans;

    public ScansConfig() {
    }

    public ScansConfig(List<ScanConfig> scans) {
        this.scans = scans;
    }

    @JsonProperty("scans")

    public List<ScanConfig> getScans() {
        return scans;
    }

    @JsonProperty("scans")

    public void setScans(List<ScanConfig> scans) {
        this.scans = scans;
    }
}