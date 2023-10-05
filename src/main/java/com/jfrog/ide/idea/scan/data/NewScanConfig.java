package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NewScanConfig {
    @JsonProperty("type")
    private SourceCodeScanType scanType;
    @JsonProperty("roots")
    private List<String> roots;
    @JsonProperty("language")
    private String language;
    @JsonProperty("output")
    private String output;
    @JsonProperty("exclude_patterns")
    private List<String> excludePatterns;
    @JsonProperty("excluded-rules")
    private List<String> excludedRules;

    public NewScanConfig(ScanConfig inputParams) {
        this(inputParams.getScanType(),
                inputParams.getRoots(),
                inputParams.getLanguage(),
                inputParams.getOutput(),
                inputParams.getSkippedFolders(),
                inputParams.getExcludedRules());
    }
}