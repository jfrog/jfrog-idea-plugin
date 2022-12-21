package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class ApplicableDetails implements Serializable {
    boolean isApplicable;
    String applicableFixReason;
    String filePathEvidence;
    String codeEvidence;
    String searchTarget;

    public ApplicableDetails(boolean isApplicable, String applicableFixReason, String filePathEvidence, String codeEvidence, String searchTarget) {
        this.isApplicable = isApplicable;
        this.applicableFixReason = applicableFixReason;
        this.filePathEvidence = filePathEvidence;
        this.codeEvidence = codeEvidence;
        this.searchTarget = searchTarget;
    }
}
