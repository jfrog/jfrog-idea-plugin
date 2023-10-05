package com.jfrog.ide.idea.ui.webview.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SastIssuePage extends IssuePage {
    @JsonProperty("analysisStep")
    private Location[] analysisSteps;
    private String ruleId;

    @SuppressWarnings("unused")
    public SastIssuePage() {
    }

    public SastIssuePage(IssuePage issuePage) {
        super(issuePage);
    }

    public SastIssuePage setAnalysisSteps(Location[] analysisSteps) {
        this.analysisSteps = analysisSteps;
        return this;
    }

    public SastIssuePage setRuleID(String ruleID) {
        this.ruleId = ruleID;
        return this;
    }
}
