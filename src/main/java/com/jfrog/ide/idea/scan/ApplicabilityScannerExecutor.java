package com.jfrog.ide.idea.scan;

import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.Output;
import com.jfrog.ide.idea.scan.data.Rule;
import com.jfrog.ide.idea.scan.data.Run;
import com.jfrog.ide.idea.scan.data.ScanConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Tal Arian
 */
public class ApplicabilityScannerExecutor extends ScanBinaryExecutor {
    private static final String SCAN_TYPE = "analyze-applicability";
    private static final String SCANNER_BINARY_NAME = "applicability_scanner";
    private static final List<String> SCANNER_ARGS = List.of("scan");

    public ApplicabilityScannerExecutor() {
        super(SCAN_TYPE, SCANNER_BINARY_NAME);
        supportedLanguages = List.of("python", "js");
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS);
    }

    @Override
    protected List<JFrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException {
        List<JFrogSecurityWarning> results = super.parseOutputSarif(outputFile);
        HashSet<String> applicabilityCves = new HashSet<>();
        results.forEach(jFrogSecurityWarning -> applicabilityCves.add(jFrogSecurityWarning.getName()));
        Output output = getOutputObj(outputFile);
        Optional<Run> run = output.getRuns().stream().findFirst();
        if (run.isPresent()) {
            List<Rule> scanners = run.get().getTool().getDriver().getRules();
            // Adds the scanner search target data
            for (JFrogSecurityWarning warning : results) {
                String ScannerSearchTarget = scanners.stream().filter(scanner -> scanner.getId().equals(warning.getName())).findFirst().map(scanner -> scanner.getFullDescription().getText()).orElse("");
                warning.setScannerSearchTarget(ScannerSearchTarget);
            }
            // Adds the not applicable CVEs data
            Stream<String> knownCves = scanners.stream().map(Rule::getId);
            knownCves.filter(cve -> !applicabilityCves.contains(cve)).forEach(cve -> results.add(new JFrogSecurityWarning(cve, false)));
        }
        return results;
    }

}
