package com.jfrog.ide.idea.ui.webview.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EosIssuePage extends IssuePage {
    @JsonProperty("analysisStep")
    private Location[] analysisSteps;

    private String[] remediation;

    @SuppressWarnings("unused")
    public EosIssuePage() {
    }

    public EosIssuePage(IssuePage issuePage) {
        super(issuePage);
    }

    @SuppressWarnings("unused")
    public Location[] getAnalysisSteps() {
        return analysisSteps;
    }

    public EosIssuePage setAnalysisSteps(Location[] analysisSteps) {
        this.analysisSteps = analysisSteps;
        return this;
    }

    @SuppressWarnings("unused")
    public String[] getRemediation() {
        return remediation;
    }

    @SuppressWarnings("unused")
    public void setRemediation(String[] remediation) {
        this.remediation = remediation;
    }
}
