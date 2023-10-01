package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

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

   public String getName() {
      return this.name;
   }

   public String getSourceRoot() {
      return this.sourceRoot;
   }

   public List<String> getExcludePatterns() {
      return this.excludePatterns;
   }

   public List<String> getExcludeScanners() {
      return this.excludeScanners;
   }

   public Map<String, ScannerConfig> getScanners() {
      return this.scanners;
   }
}
