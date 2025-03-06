package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.*;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.services.entitlements.Feature;
import lombok.Getter;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createAnonymousAccessArtifactoryManagerBuilder;
import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createArtifactoryManagerBuilder;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static com.jfrog.ide.idea.scan.utils.ScanUtils.getOSAndArc;
import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;
import static java.lang.String.join;

/**
 * @author Tal Arian
 */
public abstract class ScanBinaryExecutor {
    public static final Path BINARIES_DIR = HOME_PATH.resolve("dependencies").resolve("jfrog-security");
    private static final int UPDATE_INTERVAL = 1;
    private static final int USER_NOT_ENTITLED = 31;
    private static final int NOT_SUPPORTED = 13;
    private static final String SCANNER_BINARY_NAME = "analyzerManager";
    private static final String SCANNER_BINARY_VERSION = "1.13.4";
    private static final String BINARY_DOWNLOAD_URL = "xsc-gen-exe-analyzer-manager-local/v1/" + SCANNER_BINARY_VERSION;
    private static final String DOWNLOAD_SCANNER_NAME = "analyzerManager.zip";
    private static final String MINIMAL_XRAY_VERSION_SUPPORTED_FOR_ENTITLEMENT = "3.66.0";
    private static final String ENV_PLATFORM = "JF_PLATFORM_URL";
    private static final String ENV_USER = "JF_USER";
    private static final String ENV_PASSWORD = "JF_PASS";
    private static final String ENV_ACCESS_TOKEN = "JF_TOKEN";
    private static final String ENV_HTTP_PROXY = "HTTP_PROXY";
    private static final String ENV_HTTPS_PROXY = "HTTPS_PROXY";
    private static final String JFROG_RELEASES = "https://releases.jfrog.io/artifactory/";
    private static Path binaryTargetPath;
    private static Path archiveTargetPath;
    @Getter
    private static String osDistribution;
    private static LocalDateTime nextUpdateCheck;
    protected final SourceCodeScanType scanType;
    protected Collection<PackageManagerType> supportedPackageTypes;
    private final Log log;
    private boolean notSupported;
    private final static Object downloadLock = new Object();

    ScanBinaryExecutor(SourceCodeScanType scanType, Log log) {
        this.scanType = scanType;
        this.log = log;
        String executable = SystemUtils.IS_OS_WINDOWS ? SCANNER_BINARY_NAME + ".exe" : SCANNER_BINARY_NAME;
        binaryTargetPath = BINARIES_DIR.resolve(SCANNER_BINARY_NAME).resolve(executable);
        archiveTargetPath = BINARIES_DIR.resolve(DOWNLOAD_SCANNER_NAME);
        setOsDistribution();
    }

    private ArtifactoryManagerBuilder createManagerBuilder(boolean useJFrogReleases, ServerConfig server) {
        if (useJFrogReleases) {
            return createAnonymousAccessArtifactoryManagerBuilder(JFROG_RELEASES, server.getProxyConfForTargetUrl(JFROG_RELEASES), log);
        }
        try {
            return createArtifactoryManagerBuilder(server, log);
        } catch (Exception e) {
            log.warn(e.getMessage());
            notSupported = true;
        }
        return null;
    }

    protected void setOsDistribution() {
        try {
            osDistribution = getOSAndArc();
        } catch (IOException e) {
            log.warn(e.getMessage());
            notSupported = true;
        }
    }

    String getBinaryDownloadURL(String externalResourcesRepo) {
        String downloadUrlPrefix = "";
        if (!StringUtils.isEmpty(externalResourcesRepo)) {
            downloadUrlPrefix = String.format("%s/artifactory/", externalResourcesRepo);
        }
        return String.format("%s%s/%s/%s", downloadUrlPrefix, BINARY_DOWNLOAD_URL, getOsDistribution(), DOWNLOAD_SCANNER_NAME);
    }

    abstract Feature getScannerFeatureName();

    abstract List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException, URISyntaxException;

    protected List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, List<String> args, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException {
        return execute(inputFileBuilder, args, checkCanceled, false, indicator);
    }

    protected List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, List<String> args, Runnable checkCanceled, boolean newConfigFormat, ProgressIndicator indicator) throws IOException, InterruptedException {
        if (!shouldExecute()) {
            return List.of();
        }
        checkCanceled.run();
        updateBinaryIfNeeded();
        Path outputTempDir = null;
        Path inputFile = null;
        try {
            outputTempDir = Files.createTempDirectory("");
            Path outputFilePath = Files.createTempFile(outputTempDir, "", ".sarif");
            inputFileBuilder.output(outputFilePath.toString());
            inputFileBuilder.scanType(scanType);
            ScanConfig inputParams = inputFileBuilder.Build();
            args = new ArrayList<>(args);
            inputFile = newConfigFormat ? createTempRunInputFile(new NewScansConfig(new NewScanConfig(inputParams))) : createTempRunInputFile(new ScansConfig(List.of(inputParams)));
            args.add(inputFile.toString());
            if (newConfigFormat) {
                args.add(outputFilePath.toString());
            }

            Logger log = Logger.getInstance();
            // The following logging is done outside the commandExecutor because the commandExecutor log level is set to INFO.
            //  As it is an internal binary execution, the message should be printed for DEBUG use only.
            indicator.setText(String.format("Running %s scan at %s", scanType.toString().toLowerCase(), String.join(" ", inputParams.getRoots())));
            String cmd = String.format("%s %s", binaryTargetPath.toString(), join(" ", args));
            log.info(String.format("Executing JAS scanner %s with config: %s", cmd, inputParams));
            CommandExecutor commandExecutor = new CommandExecutor(binaryTargetPath.toString(), createEnvWithCredentials());
            CommandResults commandResults = commandExecutor.exeCommand(binaryTargetPath.toFile().getParentFile(), args,
                    null, new NullLog(), Long.MAX_VALUE, TimeUnit.MINUTES);

            checkCanceled.run();

            if (commandResults.isOk()) {
                log.info(String.format("Finished successfully to run command: %s", cmd));
                log.debug(commandResults.getRes());
                return parseOutputSarif(outputFilePath);
            }
            log.info(String.format("Failed to run command: %s", cmd));
            switch (commandResults.getExitValue()) {
                case USER_NOT_ENTITLED -> {
                    log.debug("User not entitled for advance security scan");
                    return List.of();
                }
                case NOT_SUPPORTED -> {
                    log.info(String.format("Scanner %s is not supported in the current Analyzer Manager version.", scanType));
                    return List.of();
                }
                default -> {
                    log.info(commandResults.getRes());
                    throw new IOException(commandResults.getErr());
                }
            }
        } finally {
            if (outputTempDir != null) {
                FileUtils.deleteQuietly(outputTempDir.toFile());
            }
            if (inputFile != null) {
                FileUtils.deleteQuietly(inputFile.toFile());
            }
        }
    }

    abstract List<FileTreeNode> createSpecificFileIssueNodes(List<JFrogSecurityWarning> warnings);

    private void updateBinaryIfNeeded() throws IOException {
        // Allow only one thread to check and update the binary at any time.
        synchronized (downloadLock) {
            LocalDateTime currentTime = LocalDateTime.now();
            boolean targetExists = Files.exists(binaryTargetPath);
            if (targetExists && nextUpdateCheck != null && currentTime.isBefore(nextUpdateCheck)) {
                return;
            }
            ServerConfig server = GlobalSettings.getInstance().getServerConfig();
            String externalResourcesRepo = server.getExternalResourcesRepo();
            ArtifactoryManagerBuilder artifactoryManagerBuilder = createManagerBuilder(StringUtils.isEmpty(externalResourcesRepo), server);
            try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
                if (targetExists) {
                    // Check for new version of the binary
                    try (FileInputStream archiveBinaryFile = new FileInputStream(archiveTargetPath.toFile())) {
                        String latestBinaryChecksum = getFileChecksumFromServer(artifactoryManager, externalResourcesRepo);
                        String currentBinaryCheckSum = DigestUtils.sha256Hex(archiveBinaryFile);
                        if (latestBinaryChecksum.equals(currentBinaryCheckSum)) {
                            nextUpdateCheck = currentTime.plusDays(UPDATE_INTERVAL);
                            return;
                        }
                        log.debug(String.format("Resource %s is not up to date. Downloading it.", archiveTargetPath));
                    }
                } else {
                    log.debug(String.format("Resource %s is not found. Downloading it.", binaryTargetPath));
                }
                downloadBinary(artifactoryManager, externalResourcesRepo);
            }
        }
    }

    public String getFileChecksumFromServer(ArtifactoryManager artifactoryManager, String externalResourcesRepo) throws IOException {
        String url = getBinaryDownloadURL(externalResourcesRepo);
        Header[] headers = artifactoryManager.downloadHeaders(url);
        for (Header header : headers) {
            if (StringUtils.equalsIgnoreCase(header.getName(), "x-checksum-sha256")) {
                return header.getValue();
            }
        }
        log.warn(String.format("Failed to retrieve file checksum from: %s/%s ", artifactoryManager.getUrl(), url));
        return "";
    }

    protected boolean shouldExecute() {
        if (notSupported) {
            return false;
        }
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

    protected boolean isPackageTypeSupported(PackageManagerType type) {
        return type != null && supportedPackageTypes.contains(type);
    }

    protected List<JFrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException,IndexOutOfBoundsException {
        Output output = getOutputObj(outputFile);
        List<JFrogSecurityWarning> warnings = new ArrayList<>();

        output.getRuns().forEach(run -> run.getResults().stream().filter(SarifResult::isNotSuppressed).forEach(result -> warnings.add(new JFrogSecurityWarning(result, scanType, run.getRuleFromRunById(result.getRuleId())))));

        Optional<Run> run = output.getRuns().stream().findFirst();
        if (run.isPresent()) {
            List<Rule> scanners = run.get().getTool().getDriver().getRules();
            // Adds the scanner search target data
            for (JFrogSecurityWarning warning : warnings) {
                String scannerSearchTarget = scanners.stream().filter(scanner -> scanner.getId().equals(warning.getRuleID())).findFirst().map(Rule::getFullDescription).map(Message::getText).orElse("");
                warning.setScannerSearchTarget(scannerSearchTarget);
            }
        }
        return warnings;
    }

    protected Output getOutputObj(Path outputFile) throws IOException {
        ObjectMapper om = createMapper();
        return om.readValue(outputFile.toFile(), Output.class);
    }

    protected void downloadBinary(ArtifactoryManager artifactoryManager, String externalResourcesRepo) throws IOException {
        String downloadUrl = getBinaryDownloadURL(externalResourcesRepo);
        File downloadArchive = artifactoryManager.downloadToFile(downloadUrl, archiveTargetPath.toString());
        log.debug(String.format("Downloading: %s", downloadUrl));
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

    Path createTempRunInputFile(Object scanInput) throws IOException {
        ObjectMapper om = createYAMLMapper();
        Path tempDir = Files.createTempDirectory("");
        Path inputPath = Files.createTempFile(tempDir, "", ".yaml");
        om.writeValue(inputPath.toFile(), scanInput);
        return inputPath;
    }

    private Map<String, String> createEnvWithCredentials() {
        Map<String, String> env = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
        ServerConfigImpl serverConfig = GlobalSettings.getInstance().getServerConfig();
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
        return env;
    }

}
