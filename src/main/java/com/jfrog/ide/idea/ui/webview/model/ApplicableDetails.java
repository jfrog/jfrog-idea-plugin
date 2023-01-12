package com.jfrog.ide.idea.ui.webview.model;

public class ApplicableDetails {
    private final boolean isApplicable;
    private final Evidence[] evidence;
    private final String searchTarget;

    public ApplicableDetails(boolean isApplicable, Evidence[] evidence, String searchTarget) {
        this.isApplicable = isApplicable;
        this.evidence = evidence;
        this.searchTarget = searchTarget;
    }

    @SuppressWarnings("unused")
    public boolean getIsApplicable() {
        return isApplicable;
    }

    @SuppressWarnings("unused")
    public Evidence[] getEvidence() {
        return evidence;
    }

    @SuppressWarnings("unused")
    public String getSearchTarget() {
        return searchTarget;
    }
}
