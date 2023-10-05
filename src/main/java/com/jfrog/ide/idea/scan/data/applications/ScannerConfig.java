package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ScannerConfig {
    @JsonProperty("language")
    private String language;
    @JsonProperty("working_dirs")
    private List<String> workingDirs;
    @JsonProperty("exclude_patterns")
    private List<String> excludePatterns;
    @JsonProperty("excluded_rules")
    private List<String> excludedRules;
}
