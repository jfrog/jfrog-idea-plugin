package com.jfrog.ide.idea.inspections;

import com.jfrog.ide.idea.scan.data.SarifResult;

public class JfrogSecurityWarning {
    private final int lineStart;
    private final int colStart;
    private final int lineEnd;
    private final int colEnd;
    private final String reason;
    private final String filePath;


    public JfrogSecurityWarning(
            int lineStart,
            int colStart, int lineEnd,
            int colEnd, String reason,
            String filePath
    ) {
        this.lineStart = lineStart;
        this.colStart = colStart;
        this.lineEnd = lineEnd;
        this.colEnd = colEnd;
        this.reason = reason;
        this.filePath = filePath;
    }

    public JfrogSecurityWarning(SarifResult result) {
        this(result.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine() - 1,
                result.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn(),
                result.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine() - 1,
                result.getLocations().get(0).getPhysicalLocation().getRegion().getEndColumn(),
                result.getMessage().getText(), result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri());
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

}
