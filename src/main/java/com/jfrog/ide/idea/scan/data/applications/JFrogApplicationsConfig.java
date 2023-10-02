package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class JFrogApplicationsConfig {
    @JsonProperty("version")
    private String version;
    @JsonProperty("modules")
    private List<ModuleConfig> modules;

    public JFrogApplicationsConfig(boolean createEmptyModules) {
        if (createEmptyModules) {
            modules = new ArrayList<>();
            modules.add(new ModuleConfig());
        }
    }

}
