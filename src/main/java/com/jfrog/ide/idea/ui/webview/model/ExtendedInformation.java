package com.jfrog.ide.idea.ui.webview.model;

public class ExtendedInformation {
    private final String shortDescription;
    private final String fullDescription;
    private final String jfrogResearchSeverity;
    private final String remediation;
    private final JfrogResearchSeverityReason[] jfrogResearchSeverityReason;
    
    public ExtendedInformation(String shortDescription, String fullDescription, String jfrogResearchSeverity, String remediation, JfrogResearchSeverityReason[] jfrogResearchSeverityReason) {
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.jfrogResearchSeverity = jfrogResearchSeverity;
        this.remediation = remediation;
        this.jfrogResearchSeverityReason = jfrogResearchSeverityReason;
    }

    @SuppressWarnings("unused")
    public String getShortDescription() {
        return shortDescription;
    }

    @SuppressWarnings("unused")
    public String getFullDescription() {
        return fullDescription;
    }

    @SuppressWarnings("unused")
    public String getJfrogResearchSeverity() {
        return jfrogResearchSeverity;
    }

    @SuppressWarnings("unused")
    public String getRemediation() {
        return remediation;
    }

    @SuppressWarnings("unused")
    public JfrogResearchSeverityReason[] getJfrogResearchSeverityReason() {
        return jfrogResearchSeverityReason;
    }
}
