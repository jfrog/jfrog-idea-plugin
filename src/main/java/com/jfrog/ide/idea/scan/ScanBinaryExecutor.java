package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.Output;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.services.entitlements.Feature;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.UnzipParameters;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.Header;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createAnonymousAccessArtifactoryManagerBuilder;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;

/**
 * @author Tal Arian
 */
public abstract class ScanBinaryExecutor {
    final String scanType;
    protected List<String> supportedLanguages;
    private static final Path BINARIES_DIR = HOME_PATH.resolve("dependencies").resolve("jfrog-security");
    private static Path binaryTargetPath;
    private static Path archiveTargetPath;
    private static final String MINIMAL_XRAY_VERSION_SUPPORTED_FOR_ENTITLEMENT = "3.66.0";
    private static final int UPDATE_INTERVAL = 1;
    private static LocalDateTime nextUpdateCheck;
    private final Log log;
    private boolean notSupportedOS;
    private static final String ENV_PLATFORM = "JF_PLATFORM_URL";
    private static final String ENV_USER = "JF_USER";
    private static final String ENV_PASSWORD = "JF_PASS";
    private static final String ENV_ACCESS_TOKEN = "JF_TOKEN";
    private static final String ENV_HTTP_PROXY = "HTTP_PROXY";
    private static final String ENV_HTTPS_PROXY = "HTTPS_PROXY";
    private static final String ENV_LOG_DIR = "AM_LOG_DIRECTORY";
    private static final int USER_NOT_ENTITLED = 31;
    private static final String JFROG_RELEASES = "https://releases.jfrog.io/artifactory/";
    private static String osDistribution;
    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;


    ScanBinaryExecutor(String scanType, String binaryName, String archiveName, Log log, ServerConfig server) {
        this.scanType = scanType;
        this.log = log;
        String executable = SystemUtils.IS_OS_WINDOWS ? binaryName + ".exe" : binaryName;
        binaryTargetPath = BINARIES_DIR.resolve(binaryName).resolve(executable);
        archiveTargetPath = BINARIES_DIR.resolve(archiveName);
        artifactoryManagerBuilder = createAnonymousAccessArtifactoryManagerBuilder(JFROG_RELEASES, server.getProxyConfForTargetUrl(JFROG_RELEASES), log);
        try {
            osDistribution = getOSAndArc();
        } catch (IOException e) {
            log.info(e.getMessage());
            notSupportedOS = true;
        }
    }

    public static String getOsDistribution() {
        return osDistribution;
    }

    abstract List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException, URISyntaxException;

    abstract String getBinaryDownloadURL();

    abstract Feature getScannerFeatureName();

    protected List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, List<String> args) throws IOException, InterruptedException {
        if (notSupportedOS || !shouldExecute()) {
            return List.of();
        }
        CommandExecutor commandExecutor = new CommandExecutor(binaryTargetPath.toString(), creatEnvWithCredentials());
        updateBinaryIfNeeded();
        Path outputTempDir = null;
        Path inputFile = null;
        try {
            outputTempDir = Files.createTempDirectory("");
            Path outputFilePath = Files.createTempFile(outputTempDir, "", ".sarif");
            inputFileBuilder.output(outputFilePath.toString());
            inputFileBuilder.scanType(scanType);
            inputFile = createTempRunInputFile(new ScansConfig(List.of(inputFileBuilder.Build())));
            args = new ArrayList<>(args);
            args.add(inputFile.toString());

            Logger log = Logger.getInstance();
            // Execute the external process
            CommandResults commandResults = commandExecutor.exeCommand(binaryTargetPath.toFile().getParentFile(), args, null, new NullLog());
            if (commandResults.getExitValue() == USER_NOT_ENTITLED) {
                log.debug("User not entitled for advance security scan");
                return List.of();
            }
            if (!commandResults.isOk()) {
                throw new IOException(commandResults.getErr());
            }
            return parseOutputSarif(outputFilePath);
        } finally {
            if (outputTempDir != null) {
                FileUtils.deleteQuietly(outputTempDir.toFile());
            }
            if (inputFile != null) {
                FileUtils.deleteQuietly(inputFile.toFile());
            }
        }
    }

    private void updateBinaryIfNeeded() throws IOException {
        if (!Files.exists(binaryTargetPath)) {
            downloadBinary();
            return;
        }
        if (nextUpdateCheck == null || LocalDateTime.now().isAfter(nextUpdateCheck)) {
            var currentTime = LocalDateTime.now();
            nextUpdateCheck = LocalDateTime.of(currentTime.getYear(), currentTime.getMonth(), currentTime.getDayOfMonth() + UPDATE_INTERVAL, currentTime.getHour(), currentTime.getMinute(), currentTime.getSecond());
            // Check for new version of the binary
            try (FileInputStream archiveBinaryFile = new FileInputStream(archiveTargetPath.toFile())) {
                String latestBinaryChecksum = getFileChecksumFromServer();
                String currentBinaryCheckSum = DigestUtils.sha256Hex(archiveBinaryFile);
                if (!latestBinaryChecksum.equals(currentBinaryCheckSum)) {
                    downloadBinary();
                }
            }
        }
    }

    public String getFileChecksumFromServer() throws IOException {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            Header[] headers = artifactoryManager.downloadHeaders(getBinaryDownloadURL());
            for (Header header : headers) {
                if (header.getName().toLowerCase().equals("x-checksum-sha256")) {
                    return header.getValue();
                }
            }
            return "";
        }
    }

    protected boolean shouldExecute() {
        ServerConfig server = GlobalSettings.getInstance().getServerConfig();
        try (Xray xrayClient = createXrayClientBuilder(server, log).build()) {
            try {
                if (!xrayClient.system().version().isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED_FOR_ENTITLEMENT)) {
                    return false;
                }
                return xrayClient.entitlements().isEntitled(getScannerFeatureName());
            } catch (IOException e) {
                log.error("Couldn't connect to JFrog Xray. Please check your credentials.", e);
                return false;
            }
        }
    }

    protected List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    protected List<JFrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException {
        Output output = getOutputObj(outputFile);
        List<JFrogSecurityWarning> warnings = new ArrayList<>();
        output.getRuns().forEach(run -> run.getResults().forEach(result -> warnings.add(new JFrogSecurityWarning(result))));
        return warnings;
    }

    protected Output getOutputObj(Path outputFile) throws IOException {
        ObjectMapper om = createMapper();
        return om.readValue(outputFile.toFile(), Output.class);
    }

    protected void downloadBinary() throws IOException {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            String downloadUrl = getBinaryDownloadURL();
            File downloadArchive = artifactoryManager.downloadToFile(downloadUrl, archiveTargetPath.toString());
            if (downloadArchive == null) {
                throw new IOException("An empty response received from Artifactory.");
            }
            // Delete current scanners
            FileUtils.deleteDirectory(binaryTargetPath.toFile().getParentFile());
            // Extract archive
            UnzipParameters params = new UnzipParameters();
            params.setExtractSymbolicLinks(false);
            try (ZipFile zip = new ZipFile(archiveTargetPath.toFile())) {
                zip.extractAll(binaryTargetPath.toFile().getParentFile().toString(), params);
            } catch (ZipException exception) {
                throw new IOException("An error occurred while trying to unarchived the JFrog executable:\n" + exception.getMessage());
            }
            // Set executable permissions to the downloaded scanner
            if (!binaryTargetPath.toFile().setExecutable(true)) {
                throw new IOException("An error occurred while trying to give access permissions to the JFrog executable.");
            }
        }
    }

    Path createTempRunInputFile(ScansConfig scanInput) throws IOException {
        ObjectMapper om = createYAMLMapper();
        Path tempDir = Files.createTempDirectory("");
        Path inputPath = Files.createTempFile(tempDir, "", ".yaml");
        om.writeValue(inputPath.toFile(), scanInput);
        return inputPath;
    }

    private Map<String, String> creatEnvWithCredentials() {
        Map<String, String> env = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
        ServerConfigImpl serverConfig = GlobalSettings.getInstance().getServerConfig();
        if (serverConfig.isXrayConfigured()) {
            env.put(ENV_PLATFORM, serverConfig.getUrl());
            if (StringUtils.isNotEmpty(serverConfig.getAccessToken())) {
                env.put(ENV_ACCESS_TOKEN, serverConfig.getAccessToken());
            } else {
                env.put(ENV_USER, serverConfig.getUsername());
                env.put(ENV_PASSWORD, serverConfig.getPassword());
            }

            ProxyConfiguration proxyConfiguration = serverConfig.getProxyConfForTargetUrl(serverConfig.getUrl());
            if (proxyConfiguration != null) {
                String proxyUrl = proxyConfiguration.host + ":" + proxyConfiguration.port;
                if (StringUtils.isNoneBlank(proxyConfiguration.username, proxyConfiguration.password)) {
                    proxyUrl = proxyConfiguration.username + ":" + proxyConfiguration.password + "@" + proxyUrl;
                }
                env.put(ENV_HTTP_PROXY, "http://" + proxyUrl);
                env.put(ENV_HTTPS_PROXY, "https://" + proxyUrl);
            }
        }
        env.put(ENV_LOG_DIR, BINARIES_DIR.toAbsolutePath().toString());
        return env;
    }

    private static String getOSAndArc() throws IOException {
        String arch = SystemUtils.OS_ARCH;
        // Windows
        if (SystemUtils.IS_OS_WINDOWS) {
            return "windows-amd64";
        }
        // Mac
        if (SystemUtils.IS_OS_MAC) {
            if (arch.equals("arm64")) {
                return "mac-arm64";
            } else {
                return "mac-amd64";
            }
        }
        // Linux
        if (SystemUtils.IS_OS_LINUX) {
            switch (arch) {
                case "i386":
                case "i486":
                case "i586":
                case "i686":
                case "i786":
                case "x86":
                    return "linux-386";
                case "amd64":
                case "x86_64":
                case "x64":
                    return "linux-amd64";
                case "arm":
                case "armv7l":
                    return "linux-arm";
                case "aarch64":
                    return "linux-arm64";
                case "ppc64":
                case "ppc64le":
                    return "linux-" + arch;
            }
        }
        throw new IOException(String.format("Unsupported OS: %s-%s", SystemUtils.OS_NAME, arch));
    }
}
