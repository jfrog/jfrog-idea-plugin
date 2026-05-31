package com.jfrog.ide.idea.inspections;

import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.scan.data.*;
import com.jfrog.ide.idea.utils.DescriptorPathUtils;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class JFrogSecurityWarning {
    private final int lineStart;
    private final int colStart;
    private final int lineEnd;
    private final int colEnd;
    private final String reason;
    private final String filePath;
    private final String lineSnippet;
    private String scannerSearchTarget;
    private final String ruleID;
    private final SourceCodeScanType reporter;
    private final Severity severity;
    private final FindingInfo[][] codeFlows;
    private final boolean isApplicable;

    public JFrogSecurityWarning(
            int lineStart,
            int colStart, int lineEnd,
            int colEnd, String reason,
            String filePath,
            String ruleID,
            String lineSnippet,
            SourceCodeScanType reporter,
            boolean isApplicable,
            Severity severity,
            FindingInfo[][] codeFlows
    ) {
        this.lineStart = lineStart;
        this.colStart = colStart;
        this.lineEnd = lineEnd;
        this.colEnd = colEnd;
        this.reason = reason;
        this.filePath = filePath;
        this.ruleID = ruleID;
        this.lineSnippet = lineSnippet;
        this.reporter = reporter;
        this.isApplicable = isApplicable;
        this.severity = severity;
        this.codeFlows = codeFlows;
    }

    public JFrogSecurityWarning(SarifResult result, SourceCodeScanType reporter, Rule rule) {
        this(result, reporter, rule, null);
    }

    public JFrogSecurityWarning(SarifResult result, SourceCodeScanType reporter, Rule rule, @Nullable String wslDistro) {
        this(getFirstRegion(result).getStartLine() - 1,
                getFirstRegion(result).getStartColumn() - 1,
                getFirstRegion(result).getEndLine() - 1,
                getFirstRegion(result).getEndColumn() - 1,
                determineReason(result.getMessage().getText(), rule.getShortDescription().getText(), reporter),
                getFilePath(result, wslDistro),
                result.getRuleId(),
                normalizeSnippetText(getFirstRegion(result).getSnippet().getText()),
                reporter,
                isWarningApplicable(result, rule),
                Severity.fromSarif(result.getSeverity()),
                convertCodeFlowsToFindingInfo(result.getCodeFlows(), wslDistro)
        );
    }

    private static boolean isWarningApplicable(SarifResult result, Rule rule) {
        return !result.getKind().equals("pass") && (rule.getRuleProperties().map(properties -> properties.getApplicability().equals("applicable")).orElse(true));
    }

    private static String getFilePath(SarifResult result, @Nullable String wslDistro) {
        return !result.getLocations().isEmpty()
                ? DescriptorPathUtils.sarifArtifactUriToLocalPath(
                result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri(), wslDistro)
                : "";
    }

    private static FindingInfo[][] convertCodeFlowsToFindingInfo(List<CodeFlow> codeFlows, @Nullable String wslDistro) {
        if (codeFlows == null || codeFlows.isEmpty()) {
            return null;
        }
        List<ThreadFlow> flows = codeFlows.get(0).getThreadFlows();
        if (flows == null || flows.isEmpty()) {
            return null;
        }
        FindingInfo[][] results = new FindingInfo[flows.size()][];
        for (int i = 0; i < flows.size(); i++) {
            ThreadFlow flow = flows.get(i);
            List<ThreadFlowLocation> locations = flow.getLocations();
            results[i] = new FindingInfo[locations.size()];
            for (int j = 0; j < locations.size(); j++) {
                PhysicalLocation location = locations.get(j).getLocation().getPhysicalLocation();
                results[i][j] = new FindingInfo(
                        DescriptorPathUtils.sarifArtifactUriToLocalPath(location.getArtifactLocation().getUri(), wslDistro),
                        location.getRegion().getStartLine(),
                        location.getRegion().getStartColumn(),
                        location.getRegion().getEndLine(),
                        location.getRegion().getEndColumn(),
                        normalizeSnippetText(location.getRegion().getSnippet().getText())
                );
            }
        }
        return results;
    }

    public static JFrogSecurityWarning notApplicable(String ruleId, SourceCodeScanType reporter) {
        return new JFrogSecurityWarning(0, 0, 0, 0, "", "", ruleId, "", reporter, false, Severity.Unknown, null);
    }

    public boolean isApplicable() {
        return this.isApplicable;
    }

    private static Region getFirstRegion(SarifResult result) {
        Region emptyRegion = new Region();
        emptyRegion.setSnippet(new Message());
        return !result.getLocations().isEmpty() ? result.getLocations().get(0).getPhysicalLocation().getRegion() : emptyRegion;
    }

    public void setScannerSearchTarget(String scannerSearchTarget) {
        this.scannerSearchTarget = scannerSearchTarget;
    }

    private static String determineReason(String resultMessage, String ruleMessage, SourceCodeScanType scannerType) {
        return scannerType.equals(SourceCodeScanType.SAST) ? ruleMessage : resultMessage;
    }

    private static String normalizeSnippetText(String snippetText) {
        return snippetText == null ? "" : snippetText.stripTrailing();
    }
}

