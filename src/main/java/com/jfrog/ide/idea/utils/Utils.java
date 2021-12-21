package com.jfrog.ide.idea.utils;

import com.google.common.base.Objects;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.scan.ComponentSummaryScanLogic;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.usageReport.UsageReporter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;

/**
 * Created by romang on 5/8/17.
 */
public class Utils {

    public static final Path HOME_PATH = Paths.get(System.getProperty("user.home"), ".jfrog-idea-plugin");
    public static final String PRODUCT_ID = "jfrog-idea-plugin/";
    public static final String PLUGIN_ID = "org.jfrog.idea";

    public enum ScanLogicType {GraphScan, ComponentSummary}

    public static Path getProjectBasePath(Project project) {
        return project.getBasePath() != null ? Paths.get(project.getBasePath()) : Paths.get(".");
    }

    public static boolean areRootNodesEqual(DependencyTree lhs, DependencyTree rhs) {
        GeneralInfo lhsGeneralInfo = lhs.getGeneralInfo();
        GeneralInfo rhsGeneralInfo = rhs.getGeneralInfo();
        return ObjectUtils.allNotNull(lhsGeneralInfo, rhsGeneralInfo) &&
                StringUtils.equals(lhsGeneralInfo.getPath(), rhsGeneralInfo.getPath()) &&
                StringUtils.equals(lhsGeneralInfo.getPkgType(), rhsGeneralInfo.getPkgType());
    }

    public static int getProjectIdentifier(String name, String path) {
        return Objects.hashCode(name, path);
    }

    public static int getProjectIdentifier(Project project) {
        return getProjectIdentifier(project.getName(), project.getBasePath());
    }

    public static void focusJFrogToolWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("JFrog");
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
    }

    /**
     * Get scan logic type, according to the Xray version.
     *
     * @return scan logic type, according to the Xray version.
     * @throws IOException if the version is not supported, or in case of connection error to Xray.
     */
    public static ScanLogicType getScanLogicType() throws IOException {
        ServerConfig server = GlobalSettings.getInstance().getServerConfig();
        XrayClient client = createXrayClientBuilder(server, Logger.getInstance()).build();
        Version xrayVersion = client.system().version();

        if (GraphScanLogic.isSupportedInXrayVersion(xrayVersion)) {
            return ScanLogicType.GraphScan;
        }
        if (ComponentSummaryScanLogic.isSupportedInXrayVersion(xrayVersion)) {
            return ScanLogicType.ComponentSummary;
        }
        throw new IOException("Unsupported JFrog Xray version.");
    }

    public static void sendUsageReport(String techName) {
        ServerConfigImpl serverConfig = GlobalSettings.getInstance().getServerConfig();
        Logger log = Logger.getInstance();
        if (!serverConfig.isArtifactoryConfigured()) {
            log.debug("Usage report can't be sent. Artifactory is not configured.");
            return;
        }
        String[] featureIdArray = new String[]{techName};
        IdeaPluginDescriptor jfrogPlugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        if (jfrogPlugin == null) {
            // In case we can't find the plugin version, do not send usage report.
            log.debug("Usage report can't be sent. Unknown plugin version.");
            return;
        }
        String pluginVersion = jfrogPlugin.getVersion();
        UsageReporter usageReporter = new UsageReporter(PRODUCT_ID + pluginVersion, featureIdArray);
        try {
            usageReporter.reportUsage(serverConfig.getArtifactoryUrl(), serverConfig.getUsername(), serverConfig.getPassword(), "", null, log);
        } catch (IOException e) {
            log.debug("Usage report failed: " + ExceptionUtils.getRootCauseMessage(e));
        }
        log.debug("Usage report sent successfully.");
    }
}
