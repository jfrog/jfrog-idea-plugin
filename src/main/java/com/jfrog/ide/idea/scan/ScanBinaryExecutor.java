package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Maps;
import com.jfrog.ide.idea.inspections.JfrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.Output;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import org.jfrog.build.extractor.executor.CommandExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;

/**
 * @author Tal Arian
 */
public abstract class ScanBinaryExecutor {
    protected static final Path BINARIES_DIR = HOME_PATH.resolve("dependencies").resolve("jfrog-security");
    protected final CommandExecutor commandExecutor;
    protected final String SCAN_TYPE;

    protected final String BINARY_NAME;

    protected boolean shouldExecute;

    abstract List<String> getSupportedLanguages();

    ScanBinaryExecutor(String scanType, String binaryName) {
        SCAN_TYPE = scanType;
        BINARY_NAME = binaryName;
        var binaryPath = BINARIES_DIR.resolve(BINARY_NAME);
        commandExecutor = new CommandExecutor(binaryPath.toString(), Maps.newHashMap());
        shouldExecute = Files.exists(binaryPath);
    }


    abstract List<JfrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException;

    protected List<JfrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, List<String> args) throws IOException, InterruptedException {
        if (!shouldExecute) {
            return List.of();
        }
        var outputTempDir = Files.createTempDirectory("");
        var outputFilePath = Files.createTempFile(outputTempDir, "", ".sarif");
        inputFileBuilder.output(outputFilePath.toString());
        inputFileBuilder.scanType(SCAN_TYPE);
        var inputFile = createTempRunInputFile(new ScansConfig(List.of(inputFileBuilder.Build())));
        args = new ArrayList<>(args);
        args.add(inputFile.toString());

        var log = Logger.getInstance();
        // Execute the external process
        var commandResults = this.commandExecutor.exeCommand(outputTempDir.toFile(), args, null, log);
        if (!commandResults.isOk()) {
            log.error(commandResults.getRes());
            log.error(commandResults.getErr());
            // No output to parse, return an empty list.
            return List.of();
        }
        return parseOutputSarif(outputFilePath);
    }

    protected List<JfrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException {
        List<JfrogSecurityWarning> warnings = new ArrayList<>();
        ObjectMapper om = new ObjectMapper(new JsonFactory());
        Output output = om.readValue(outputFile.toFile(), Output.class);
        output.getRuns().forEach(run -> run.getResults().forEach(result -> warnings.add(new JfrogSecurityWarning(result))));
        return warnings;
    }

    protected Path createTempRunInputFile(ScansConfig scanInput) throws IOException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        var tempDir = Files.createTempDirectory("");
        var inputPath = Files.createTempFile(tempDir, "", ".yaml");
        om.writeValue(inputPath.toFile(), scanInput);
        return inputPath;
    }
}
