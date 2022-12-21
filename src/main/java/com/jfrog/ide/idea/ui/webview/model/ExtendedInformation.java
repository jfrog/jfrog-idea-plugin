package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class ExtendedInformation implements Serializable {

    String shortDescription;
    String fullDescription;
    String jfrogResearchSeverity;
    String remediation;
    JfrogResearchSeverityReason[] jfrogResearchSeverityReason;

    public ExtendedInformation() {
    }
    
    public ExtendedInformation(String shortDescription, String fullDescription, String jfrogResearchSeverity, String remediation, JfrogResearchSeverityReason[] jfrogResearchSeverityReason) {
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.jfrogResearchSeverity = jfrogResearchSeverity;
        this.remediation = remediation;
        this.jfrogResearchSeverityReason = jfrogResearchSeverityReason;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getJfrogResearchSeverity() {
        return jfrogResearchSeverity;
    }

    public void setJfrogResearchSeverity(String jfrogResearchSeverity) {
        this.jfrogResearchSeverity = jfrogResearchSeverity;
    }

    public String getRemediation() {
        return remediation;
    }

    public void setRemediation(String remediation) {
        this.remediation = remediation;
    }

    public JfrogResearchSeverityReason[] getJfrogResearchSeverityReason() {
        return jfrogResearchSeverityReason;
    }

    public void setJfrogResearchSeverityReason(JfrogResearchSeverityReason[] jfrogResearchSeverityReason) {
        this.jfrogResearchSeverityReason = jfrogResearchSeverityReason;
    }
}
