package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.Output;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;
import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;

/**
 * @author Tal Arian
 */
public abstract class ScanBinaryExecutor {

    protected List<String> supportedLanguages;

    final String scanType;
    private static final Path BINARIES_DIR = HOME_PATH.resolve("dependencies").resolve("jfrog-security");
    private final String BINARY_NAME;
    private final CommandExecutor commandExecutor;
    private boolean shouldExecute;

    ScanBinaryExecutor(String scanType, String binaryName) {
        this.scanType = scanType;
        if (SystemUtils.IS_OS_WINDOWS) {
            binaryName += ".exe";
        }
        BINARY_NAME = binaryName;
        Path binaryPath = BINARIES_DIR.resolve(BINARY_NAME);
        commandExecutor = new CommandExecutor(binaryPath.toString(), Maps.newHashMap());
        shouldExecute = Files.exists(binaryPath);
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
