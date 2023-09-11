package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class NewScansConfig {
    @JsonProperty("scans")
    private List<NewScanConfig> scans;

    @SuppressWarnings("unused")
    public NewScansConfig() {
    }

    public NewScansConfig(NewScanConfig scan) {
        this.scans = List.of(scan);
    }
}