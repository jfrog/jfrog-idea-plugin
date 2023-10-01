package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ScannerConfig {
   @JsonProperty("language")
   private String language;
   @JsonProperty("working_dirs")
   private List<String> workingDirs;
   @JsonProperty("exclude_patterns")
   private List<String> excludePatterns;
   @JsonProperty("excluded_rules")
   private List<String> excludedRules;

   public String getLanguage() {
      return this.language;
   }

   public List<String> getWorkingDirs() {
      return this.workingDirs;
   }

   public List<String> getExcludePatterns() {
      return this.excludePatterns;
   }

   public List<String> getExcludedRules() {
      return this.excludedRules;
   }
}
