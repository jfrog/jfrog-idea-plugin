package com.jfrog.ide.idea.ui.webview.model;

public class Evidence {
    private final String reason;
    private final String filePathEvidence;
    private final String codeEvidence;

    public Evidence(String reason, String filePathEvidence, String codeEvidence) {
        this.reason = reason;
        this.filePathEvidence = filePathEvidence;
        this.codeEvidence = codeEvidence;
    }

    @SuppressWarnings("unused")
    public String getReason() {
        return reason;
    }

    @SuppressWarnings("unused")
    public String getFilePathEvidence() {
        return filePathEvidence;
    }

    @SuppressWarnings("unused")
    public String getCodeEvidence() {
        return codeEvidence;
    }
}
