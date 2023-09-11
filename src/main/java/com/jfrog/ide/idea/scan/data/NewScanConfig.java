package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import lombok.Getter;

import java.util.List;

@Getter
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

    @SuppressWarnings("unused")
    public NewScanConfig() {
    }

    public NewScanConfig(SourceCodeScanType scanType, List<String> roots, String language, List<String> excludePatterns, List<String> excludedRules, String output) {
        this.scanType = scanType;
        this.roots = roots;
        this.language = language;
        this.excludePatterns = excludePatterns;
        this.excludedRules = excludedRules;
        this.output = output;
    }

    public NewScanConfig(ScanConfig inputParams) {
        this(inputParams.getScanType(),
                inputParams.getRoots(),
                inputParams.getLanguage(),
                inputParams.getSkippedFolders(), inputParams.getExcludedRules(),
                inputParams.getOutput());
    }
}