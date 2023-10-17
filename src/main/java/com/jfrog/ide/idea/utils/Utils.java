package com.jfrog.ide.idea.utils;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jfrog.ide.common.utils.usage.EcosystemUsageReporter;
import com.jfrog.ide.common.utils.usage.UsageReport;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.log.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.usageReport.ClientIdUsageReporter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import static com.jfrog.ide.common.utils.Utils.createSSLContext;
import static com.jfrog.ide.common.utils.Utils.resolveArtifactoryUrl;

/**
 * Created by romang on 5/8/17.
 */
public class Utils {

    public static final Path HOME_PATH = Paths.get(System.getProperty("user.home"), ".jfrog-idea-plugin");
    public static final String PRODUCT_ID = "jfrog-idea-plugin";
    public static final String PLUGIN_ID = "org.jfrog.idea";

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

    public static void focusJFrogToolWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("JFrog");
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
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
        ClientIdUsageReporter artifactoryUsageReporter = new ClientIdUsageReporter(PRODUCT_ID + "/" + pluginVersion, featureIdArray, log);
        EcosystemUsageReporter ecosystemUsageReporter = new EcosystemUsageReporter(log);
        String artifactoryUrl = resolveArtifactoryUrl(serverConfig.getArtifactoryUrl(), serverConfig.getUrl());
        try {
            artifactoryUsageReporter.reportUsage(artifactoryUrl, serverConfig.getUsername(), serverConfig.getPassword(), serverConfig.getAccessToken(), serverConfig.getProxyConfForTargetUrl(artifactoryUrl), createSSLContext(serverConfig), log);
            ecosystemUsageReporter.reportUsage(new UsageReport(PRODUCT_ID, new String(DigestUtils.md5(serverConfig.getXrayUrl())), artifactoryUsageReporter.getUniqueClientId(), featureIdArray), createSSLContext(serverConfig));
        } catch (IOException | RuntimeException | NoSuchAlgorithmException | KeyStoreException |
                 KeyManagementException e) {
            log.debug("Usage report failed: " + ExceptionUtils.getRootCauseMessage(e));
        }
        log.debug("Usage report sent successfully.");
    }

    /**
     * Return true if the input URL is valid.
     *
     * @param urlStr - The URL to check
     * @return true if the input URL is valid.
     */
    public static boolean isValidUrl(String urlStr) {
        try {
            new URL(urlStr).toURI();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    /**
     * Walk on each file in the resource path and copy files recursively to the target directory.
     *
     * @param resourceName - Abs path in resources begins with '/'
     * @param targetDir    - Destination directory
     * @throws URISyntaxException in case of error in converting the URL to URI.
     * @throws IOException        in case of any unexpected I/O error.
     */
    public static void extractFromResources(String resourceName, Path targetDir) throws URISyntaxException, IOException {
        URL resource = Utils.class.getResource(resourceName);
        if (resource == null) {
            throw new IOException("Resource '" + resourceName + "' was not found");
        }
        try (FileSystem fileSystem = FileSystems.newFileSystem(resource.toURI(), Collections.emptyMap())) {
            Path jarPath = fileSystem.getPath(resourceName);
            Files.walkFileTree(jarPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(targetDir.resolve(jarPath.relativize(dir).toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, targetDir.resolve(jarPath.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
