package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class JFrogApplicationsConfig {
   @JsonProperty("version")
   private String version;
   @JsonProperty("modules")
   private List<ModuleConfig> modules;

   public String getVersion() {
      return this.version;
   }

   public List<ModuleConfig> getModules() {
      return this.modules;
   }
}
