package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ModuleConfig {
    @JsonProperty("name")
    private String name;
    @JsonProperty("source_root")
    private String sourceRoot;
    @JsonProperty("exclude_patterns")
    private List<String> excludePatterns;
    @JsonProperty("exclude_scanners")
    private List<String> excludeScanners;
    @JsonProperty("scanners")
    private Map<String, ScannerConfig> scanners;
}
