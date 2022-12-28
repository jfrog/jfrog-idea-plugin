package com.jfrog.ide.idea.inspections;

import com.jfrog.ide.idea.scan.data.Region;
import com.jfrog.ide.idea.scan.data.SarifResult;
import org.apache.commons.lang.StringUtils;

public class JFrogSecurityWarning {
    private final int lineStart;
    private final int colStart;
    private final int lineEnd;
    private final int colEnd;
    private final String reason;
    private final String filePath;
    private final String lineSnippet;
    private final String name;

    public JFrogSecurityWarning(
            int lineStart,
            int colStart, int lineEnd,
            int colEnd, String reason,
            String filePath,
            String name,
            String lineSnippet
    ) {
        this.lineStart = lineStart;
        this.colStart = colStart;
        this.lineEnd = lineEnd;
        this.colEnd = colEnd;
        this.reason = reason;
        this.filePath = filePath;
        this.name = name;
        this.lineSnippet = lineSnippet;
    }

    public JFrogSecurityWarning(SarifResult result) {
        this(getFirstRegion(result).getStartLine() - 1,
                getFirstRegion(result).getStartColumn(),
                getFirstRegion(result).getEndLine() - 1,
                getFirstRegion(result).getEndColumn(),
                result.getMessage().getText(),
                StringUtils.removeStart(result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri(), "file://"),
                result.getRuleId(),
                getFirstRegion(result).getSnippet().getText());
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

    public String getName() {
        return name;
    }

    public String getLineSnippet() {
        return lineSnippet;
    }

    private static Region getFirstRegion(SarifResult result) {
        return result.getLocations().get(0).getPhysicalLocation().getRegion();
    }
}
