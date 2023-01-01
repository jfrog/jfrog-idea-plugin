package com.jfrog.ide.idea.ui.webview.model;

public class Evidence {
    String reason;
    String filePathEvidence;
    String codeEvidence;

    public Evidence(String reason, String filePathEvidence, String codeEvidence) {
        this.reason = reason;
        this.filePathEvidence = filePathEvidence;
        this.codeEvidence = codeEvidence;
    }
}
