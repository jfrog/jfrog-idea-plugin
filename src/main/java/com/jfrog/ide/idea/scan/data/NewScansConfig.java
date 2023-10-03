package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NewScansConfig {
    @JsonProperty("scans")
    private List<NewScanConfig> scans;

    public NewScansConfig(NewScanConfig scan) {
        this.scans = List.of(scan);
    }
}