package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class ApplicableDetails implements Serializable {
    public boolean getIsApplicable() {
        return isApplicable;
    }

    public void setIsApplicable(boolean applicable) {
        isApplicable = applicable;
    }

    public Evidence[] getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence[] evidence) {
        this.evidence = evidence;
    }

    public String getSearchTarget() {
        return searchTarget;
    }

    public void setSearchTarget(String searchTarget) {
        this.searchTarget = searchTarget;
    }

    boolean isApplicable;
    Evidence[] evidence;
    String searchTarget;

    public ApplicableDetails(boolean isApplicable, Evidence[] evidence, String searchTarget) {
        this.isApplicable = isApplicable;
        this.evidence = evidence;
        this.searchTarget = searchTarget;
    }
}
