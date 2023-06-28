package com.jfrog.ide.idea.inspections;

import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.idea.scan.data.Message;
import com.jfrog.ide.idea.scan.data.Region;
import com.jfrog.ide.idea.scan.data.SarifResult;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import org.apache.commons.lang.StringUtils;

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
            Severity severity
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
    }

    public JFrogSecurityWarning(SarifResult result, SourceCodeScanType reporter) {
        this(getFirstRegion(result).getStartLine() - 1,
                getFirstRegion(result).getStartColumn() - 1,
                getFirstRegion(result).getEndLine() - 1,
                getFirstRegion(result).getEndColumn() - 1,
                result.getMessage().getText(),
                result.getLocations().size() > 0 ? StringUtils.removeStart(result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri(), "file://") : "",
                result.getRuleId(),
                getFirstRegion(result).getSnippet().getText(),
                reporter,
                !result.getKind().equals("pass"),
                Severity.fromSarif(result.getSeverity()));
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
        return result.getLocations().size() > 0 ? result.getLocations().get(0).getPhysicalLocation().getRegion() : emptyRegion;
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
}
