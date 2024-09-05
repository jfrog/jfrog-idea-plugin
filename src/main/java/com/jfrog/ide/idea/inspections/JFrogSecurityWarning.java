package com.jfrog.ide.idea.inspections;

import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.scan.data.*;
import lombok.Getter;

import java.net.URI;
import java.nio.file.Paths;
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
        this(getFirstRegion(result).getStartLine() - 1,
                getFirstRegion(result).getStartColumn() - 1,
                getFirstRegion(result).getEndLine() - 1,
                getFirstRegion(result).getEndColumn() - 1,
                result.getMessage().getText(),
                getFilePath(result),
                result.getRuleId(),
                getFirstRegion(result).getSnippet().getText(),
                reporter,
                isWarningApplicable(result,rule),
                Severity.fromSarif(result.getSeverity()),
                convertCodeFlowsToFindingInfo(result.getCodeFlows())
        );
    }

    private static boolean isWarningApplicable(SarifResult result,Rule rule){
       return !result.getKind().equals("pass") && (rule.getRuleProperties().map(properties -> properties.getApplicability().equals("applicable")).orElse(true));
    }

    private static String getFilePath(SarifResult result){
       return !result.getLocations().isEmpty() ? uriToPath(result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri()) : "";
    }

    private static FindingInfo[][] convertCodeFlowsToFindingInfo(List<CodeFlow> codeFlows) {
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
                        uriToPath(location.getArtifactLocation().getUri()),
                        location.getRegion().getStartLine(),
                        location.getRegion().getStartColumn(),
                        location.getRegion().getEndLine(),
                        location.getRegion().getEndColumn(),
                        location.getRegion().getSnippet().getText()
                );
            }
        }
        return results;
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

    private static String uriToPath(String path) {
        return Paths.get(URI.create(path)).toString();
    }
}

