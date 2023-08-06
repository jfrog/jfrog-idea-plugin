package com.jfrog.ide.idea.ui.webview.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class EosIssuePage extends IssuePage {
    @JsonProperty("analysisStep")
    private Location[] analysisSteps;
    private String[] remediation;
    private String ruleId;

    @SuppressWarnings("unused")
    public EosIssuePage() {
    }

    public EosIssuePage(IssuePage issuePage) {
        super(issuePage);
    }

    public EosIssuePage setAnalysisSteps(Location[] analysisSteps) {
        this.analysisSteps = analysisSteps;
        return this;
    }

    public EosIssuePage setRuleID(String ruleID) {
        this.ruleId = ruleID;
        return this;
    }
}
