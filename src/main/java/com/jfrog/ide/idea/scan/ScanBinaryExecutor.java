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

    ScanBinaryExecutor(String scanType, String binaryName) {
        SCAN_TYPE = scanType;
        BINARY_NAME = binaryName;
        commandExecutor = new CommandExecutor(BINARIES_DIR.resolve(BINARY_NAME).toString(), Maps.newHashMap());
    }


    abstract List<JfrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException;

    protected List<JfrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, List<String> args, boolean eos) throws IOException, InterruptedException {
        var outputTempDir = Files.createTempDirectory("");
        var outputFilePath = Files.createTempFile(outputTempDir, "", ".sarif");
        // TODO: Remove this variable after JFrog security team unified output and sarif-output vars
        if (eos) {
            inputFileBuilder.output(outputFilePath.toString());
        } else {
            inputFileBuilder.sarifOutput(outputFilePath.toString());
        }
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

    private List<JfrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException {
        List<JfrogSecurityWarning> warnings = new ArrayList<>();
        ObjectMapper om = new ObjectMapper(new JsonFactory());
        Output output = om.readValue(outputFile.toFile(), Output.class);
        output.getRuns().forEach(run -> run.getResults().forEach(result -> warnings.add(new JfrogSecurityWarning(result))));
        return warnings;
    }

    private Path createTempRunInputFile(ScansConfig scanInput) throws IOException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        var tempDir = Files.createTempDirectory("");
        var inputPath = Files.createTempFile(tempDir, "", ".yaml");
        om.writeValue(inputPath.toFile(), scanInput);
        return inputPath;
    }
}
