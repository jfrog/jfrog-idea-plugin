package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({
        "scans",
})
public class ScansConfig {
    @JsonProperty("scans")
    private List<ScanConfig> scans;

    @SuppressWarnings("UnusedReturnValue")
    public ScansConfig() {
    }

    public ScansConfig(List<ScanConfig> scans) {
        this.scans = scans;
    }

    public List<ScanConfig> getScans() {
        return scans;
    }

    @SuppressWarnings("UnusedReturnValue")
    public void setScans(List<ScanConfig> scans) {
        this.scans = scans;
    }
}