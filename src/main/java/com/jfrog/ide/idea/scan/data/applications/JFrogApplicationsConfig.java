package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.jfrog.ide.idea.scan.SourceCodeScannerManager.convertToSkippedFolders;

@Getter
@NoArgsConstructor
public class JFrogApplicationsConfig {
    @JsonProperty("version")
    private String version;
    @JsonProperty("modules")
    private List<ModuleConfig> modules;

    public static JFrogApplicationsConfig createApplicationConfigWithDefaultModule(Project project) {
        JFrogApplicationsConfig applicationsConfig = new JFrogApplicationsConfig();
        Set<Path> paths = com.jfrog.ide.idea.scan.ScanUtils.createScanPaths(project);
        applicationsConfig.modules = new ArrayList<>();

        for (Path path : paths) {
            ModuleConfig defualtModuleConfig = new ModuleConfig();
            defualtModuleConfig.setSourceRoot(path.toString());
            defualtModuleConfig.setExcludePatterns(convertToSkippedFolders(GlobalSettings.getInstance().getServerConfig().getExcludedPaths()));
            applicationsConfig.modules.add(defualtModuleConfig);
        }

        return applicationsConfig;
    }
}
