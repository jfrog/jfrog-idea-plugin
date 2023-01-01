package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ScansConfig {
    @JsonProperty("scans")
    private List<ScanConfig> scans;

    @SuppressWarnings("unused")
    public ScansConfig() {
    }

    public ScansConfig(List<ScanConfig> scans) {
        this.scans = scans;
    }

    public List<ScanConfig> getScans() {
        return scans;
    }

    @SuppressWarnings("unused")
    public void setScans(List<ScanConfig> scans) {
        this.scans = scans;
    }
}