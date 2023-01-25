package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.Output;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;
import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;

/**
 * @author Tal Arian
 */
public abstract class ScanBinaryExecutor {
    private static final Path BINARIES_DIR = HOME_PATH.resolve("dependencies").resolve("jfrog-security");
    private final CommandExecutor commandExecutor;
    final String scanType;
    protected List<String> supportedLanguages;
    private final boolean shouldExecute;

    private static final String ENV_PLATFORM = "JF_PLATFORM_URL";
    private static final String ENV_USER = "JF_USER";
    private static final String ENV_PASSWORD = "JF_PASS";
    private static final String ENV_ACCESS_TOKEN = "JF_TOKEN";
    private static final String ENV_HTTP_PROXY = "HTTP_PROXY";
    private static final String ENV_HTTPS_PROXY = "HTTPS_PROXY";


    ScanBinaryExecutor(String scanType, String binaryName) {
        this.scanType = scanType;
        if (SystemUtils.IS_OS_WINDOWS) {
            binaryName += ".exe";
        }
        Path binaryPath = BINARIES_DIR.resolve(binaryName);
        commandExecutor = new CommandExecutor(binaryPath.toString(), creatEnvWithCredentials());
        shouldExecute = Files.exists(binaryPath);
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
        return env;
    }

    abstract List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException;

    protected List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, List<String> args) throws IOException, InterruptedException {
        if (!shouldExecute) {
            return List.of();
        }
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
            CommandResults commandResults = this.commandExecutor.exeCommand(outputTempDir.toFile(), args, null, log);
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

    Path createTempRunInputFile(ScansConfig scanInput) throws IOException {
        ObjectMapper om = createYAMLMapper();
        Path tempDir = Files.createTempDirectory("");
        Path inputPath = Files.createTempFile(tempDir, "", ".yaml");
        om.writeValue(inputPath.toFile(), scanInput);
        return inputPath;
    }
}
