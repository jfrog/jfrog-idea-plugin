package com.jfrog.ide.idea.inspections;

import com.jfrog.ide.common.nodes.subentities.FindingInfo;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.scan.data.*;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class JFrogSecurityWarning {
    private final int lineStart;
    private final int colStart;
    private final int lineEnd;
    private final int colEnd;
    private final String reason;
    private final String filePath;
    private final String lineSnippet;
    private String scannerSearchTarget;
    private final String name;
    private final SourceCodeScanType reporter;
    private final Severity severity;

    private final FindingInfo[][] codeFlows;

    private final boolean isApplicable;

    public JFrogSecurityWarning(
            int lineStart,
            int colStart, int lineEnd,
            int colEnd, String reason,
            String filePath,
            String name,
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
        this.name = name;
        this.lineSnippet = lineSnippet;
        this.reporter = reporter;
        this.isApplicable = isApplicable;
        this.severity = severity;
        this.codeFlows = codeFlows;
    }

    public JFrogSecurityWarning(SarifResult result, SourceCodeScanType reporter) {
        this(getFirstRegion(result).getStartLine() - 1,
                getFirstRegion(result).getStartColumn() - 1,
                getFirstRegion(result).getEndLine() - 1,
                getFirstRegion(result).getEndColumn() - 1,
                result.getMessage().getText(),
                !result.getLocations().isEmpty() ? StringUtils.removeStart(result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri(), "file://") : "",
                result.getRuleId(),
                getFirstRegion(result).getSnippet().getText(),
                reporter,
                !result.getKind().equals("pass"),
                Severity.fromSarif(result.getSeverity()),
                convertCodeFlowsToFindingInfo(result.getCodeFlows())
        );
    }

    private static FindingInfo[][] convertCodeFlowsToFindingInfo(List<CodeFlow> codeFlows) {
        if (codeFlows.isEmpty()) {
            return null;
        }
        List<ThreadFlow> flows = codeFlows.get(0).getThreadFlows();
        if (flows.isEmpty()) {
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
                        location.getArtifactLocation().getUri(),
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

    public int getLineStart() {
        return lineStart;
    }

    public int getColStart() {
        return colStart;
    }

    public int getLineEnd() {
        return lineEnd;
    }

    public int getColEnd() {
        return colEnd;
    }

    public String getReason() {
        return reason;
    }

    public String getFilePath() {
        return filePath;
    }

    public SourceCodeScanType getReporter() {
        return reporter;
    }

    public String getLineSnippet() {
        return lineSnippet;
    }

    public boolean isApplicable() {
        return this.isApplicable;
    }

    private static Region getFirstRegion(SarifResult result) {
        Region emptyRegion = new Region();
        emptyRegion.setSnippet(new Message());
        return !result.getLocations().isEmpty() ? result.getLocations().get(0).getPhysicalLocation().getRegion() : emptyRegion;
    }

    public String getScannerSearchTarget() {
        return scannerSearchTarget;
    }

    public void setScannerSearchTarget(String scannerSearchTarget) {
        this.scannerSearchTarget = scannerSearchTarget;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getName() {
        return name;
    }

    public FindingInfo[][] getCodeFlows() {
        return codeFlows;
    }
}
