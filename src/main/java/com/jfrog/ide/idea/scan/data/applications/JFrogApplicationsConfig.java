package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.idea.scan.SourceCodeScannerManager.convertToSkippedFolders;
import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;

@Getter
@NoArgsConstructor
public class JFrogApplicationsConfig {
    @JsonProperty("version")
    private String version;
    @JsonProperty("modules")
    private List<ModuleConfig> modules;

    public static JFrogApplicationsConfig createApplicationConfigWithDefaultModule(Project project) {
        JFrogApplicationsConfig applicationsConfig = new JFrogApplicationsConfig();

        ModuleConfig defualtModuleConfig = new ModuleConfig();
        defualtModuleConfig.setSourceRoot(getProjectBasePath(project).toAbsolutePath().toString());
        defualtModuleConfig.setExcludePatterns(convertToSkippedFolders(GlobalSettings.getInstance().getServerConfig().getExcludedPaths()));

        applicationsConfig.modules = new ArrayList<>();
        applicationsConfig.modules.add(defualtModuleConfig);

        return applicationsConfig;
    }

}
