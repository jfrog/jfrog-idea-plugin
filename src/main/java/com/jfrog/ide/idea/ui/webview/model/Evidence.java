package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class Evidence implements Serializable {
    String reason;
    String filePathEvidence;
    String codeEvidence;

    public Evidence() {

    }

    public Evidence(String reason, String filePathEvidence, String codeEvidence) {
        this.reason = reason;
        this.filePathEvidence = filePathEvidence;
        this.codeEvidence = codeEvidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getFilePathEvidence() {
        return filePathEvidence;
    }

    public void setFilePathEvidence(String filePathEvidence) {
        this.filePathEvidence = filePathEvidence;
    }

    public String getCodeEvidence() {
        return codeEvidence;
    }

    public void setCodeEvidence(String codeEvidence) {
        this.codeEvidence = codeEvidence;
    }
}
