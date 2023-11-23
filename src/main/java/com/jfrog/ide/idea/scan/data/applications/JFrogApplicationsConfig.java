package com.jfrog.ide.idea.scan.data.applications;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.scan.utils.ScanUtils;
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
        Set<Path> paths = ScanUtils.createScanPaths(project);
        applicationsConfig.modules = new ArrayList<>();

        for (Path path : paths) {
            ModuleConfig defaultModuleConfig = new ModuleConfig();
            defaultModuleConfig.setSourceRoot(path.toString());
            defaultModuleConfig.setExcludePatterns(convertToSkippedFolders(GlobalSettings.getInstance().getServerConfig().getExcludedPaths()));
            applicationsConfig.modules.add(defaultModuleConfig);
        }

        return applicationsConfig;
    }
}
