package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class JfrogResearchSeverityReason implements Serializable {
    String name;
    String description;
    boolean isPositive;

    public JfrogResearchSeverityReason() {
    }
    
    public JfrogResearchSeverityReason(String name, String description, boolean isPositive) {
        this.name = name;
        this.description = description;
        this.isPositive = isPositive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getIsPositive() {
        return isPositive;
    }

    public void setIsPositive(String isPositive) {
        isPositive = isPositive;
    }
}
