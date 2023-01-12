package com.jfrog.ide.idea.ui.webview.model;

public class JfrogResearchSeverityReason {
    private final String name;
    private final String description;
    private final boolean isPositive;
    
    public JfrogResearchSeverityReason(String name, String description, boolean isPositive) {
        this.name = name;
        this.description = description;
        this.isPositive = isPositive;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public boolean getIsPositive() {
        return isPositive;
    }
}
