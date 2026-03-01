package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.ApplicableIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.VulnerabilityNode;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.*;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Tal Arian
 */
public class ApplicabilityScannerExecutor extends ScanBinaryExecutor {
    private static final List<String> SCANNER_ARGS = List.of("ca");
    private static final List<PackageManagerType> SUPPORTED_PACKAGE_TYPES = List.of(PackageManagerType.PYPI, PackageManagerType.NPM, PackageManagerType.YARN, PackageManagerType.GRADLE, PackageManagerType.MAVEN);

    public ApplicabilityScannerExecutor(Log log) {
        super(SourceCodeScanType.CONTEXTUAL, log);
        supportedPackageTypes = SUPPORTED_PACKAGE_TYPES;
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled, indicator);
    }

    @Override
    protected List<JFrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException, IndexOutOfBoundsException {
        Output output = getOutputObj(outputFile);
        List<JFrogSecurityWarning> warnings = new ArrayList<>();
        for (Run run : output.getRuns()) {
            List<Rule> rules = run.getTool().getDriver().getRules();
            Map<String, List<SarifResult>> resultsByRule = run.getResults().stream()
                    .filter(SarifResult::isNotSuppressed)
                    .filter(r -> !"informational".equals(r.getKind()))
                    .collect(Collectors.groupingBy(SarifResult::getRuleId));

            for (Rule rule : getUniqueRules(rules)) {
                Optional<RuleProperties> props = rule.getRuleProperties();
                if (props.isEmpty()) {
                    continue;
                }
                String applicability = props.get().getApplicability();
                if (applicability == null) {
                    continue;
                }

                if ("applicable".equals(applicability)) {
                    List<SarifResult> evidence = resultsByRule.getOrDefault(rule.getId(), List.of());
                    for (SarifResult result : evidence) {
                        if (!result.getLocations().isEmpty()) {
                            warnings.add(new JFrogSecurityWarning(result, scanType, rule));
                        }
                    }
                } else if ("not_applicable".equals(applicability)) {
                    warnings.add(JFrogSecurityWarning.notApplicable(rule.getId(), scanType));
                }
            }
        }
        return warnings;
    }

    private List<Rule> getUniqueRules(List<Rule> rules) {
        Map<String, Rule> ruleMap = new LinkedHashMap<>();
        for (Rule rule : rules) {
            Rule existing = ruleMap.get(rule.getId());
            if (existing == null) {
                ruleMap.put(rule.getId(), rule);
            } else {
                Optional<RuleProperties> existingProps = existing.getRuleProperties();
                if (existingProps.isPresent() && "not_applicable".equals(existingProps.get().getApplicability())) {
                    Optional<RuleProperties> currentProps = rule.getRuleProperties();
                    if (currentProps.isPresent() && !"not_applicable".equals(currentProps.get().getApplicability())) {
                        ruleMap.put(rule.getId(), rule);
                    }
                }
            }
        }
        return new ArrayList<>(ruleMap.values());
    }

    @Override
    List<FileTreeNode> createSpecificFileIssueNodes(List<JFrogSecurityWarning> warnings) {
        return createSpecificFileIssueNodes(warnings, new HashMap<>());
    }

    List<FileTreeNode> createSpecificFileIssueNodes(List<JFrogSecurityWarning> warnings, Map<String, List<VulnerabilityNode>> issuesMap) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : warnings) {
            // Update all VulnerabilityNodes that have the warning's CVE
            String cve = StringUtils.removeStart(warning.getRuleID(), "applic_");
            List<VulnerabilityNode> issues = issuesMap.get(cve);
            if (issues != null) {
                if (warning.isApplicable()) {
                    // Create FileTreeNodes for files with applicable issues
                    FileTreeNode fileNode = results.get(warning.getFilePath());
                    if (fileNode == null) {
                        fileNode = new FileTreeNode(warning.getFilePath());
                        results.put(warning.getFilePath(), fileNode);
                    }

                    ApplicableIssueNode applicableIssue = new ApplicableIssueNode(
                            cve, warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                            warning.getFilePath(), warning.getReason(), warning.getLineSnippet(), warning.getScannerSearchTarget(),
                            issues.get(0), warning.getRuleID());
                    fileNode.addIssue(applicableIssue);
                    for (VulnerabilityNode issue : issues) {
                        issue.updateApplicableInfo(applicableIssue);
                    }
                } else {
                    // Mark non-applicable vulnerabilities.
                    for (VulnerabilityNode issue : issues) {
                        issue.setNotApplicable();
                    }
                }
            }
        }
        return new ArrayList<>(results.values());
    }

    @Override
    Feature getScannerFeatureName() {
        return Feature.CONTEXTUAL_ANALYSIS;
    }
}
