package com.jfrog.ide.idea.ui.webview.model;

public class IssuePage {
    private String pageType;
    private String header;
    private String severity;
    private String abbreviation;
    private Location location;
    private String description;
    private Finding finding;

    public IssuePage() {
    }

    @SuppressWarnings("unused")
    public String getHeader() {
        return header;
    }

    public IssuePage header(String header) {
        this.header = header;
        return this;
    }

    @SuppressWarnings("unused")
    public String getAbbreviation() {
        return abbreviation;
    }

    public IssuePage abbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
        return this;
    }

    @SuppressWarnings("unused")
    public String getPageType() {
        return pageType;
    }

    public IssuePage type(String type) {
        this.pageType = type;
        return this;
    }

    @SuppressWarnings("unused")
    public String getDescription() {
        return description;
    }

    public IssuePage description(String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public Location getLocation() {
        return location;
    }

    public IssuePage location(Location location) {
        this.location = location;
        return this;
    }

    @SuppressWarnings("unused")
    public String getSeverity() {
        return severity;
    }

    public IssuePage severity(String severity) {
        this.severity = severity;
        return this;
    }

    @SuppressWarnings("unused")
    public Finding getFinding() {
        return finding;
    }

    public IssuePage finding(Finding finding) {
        this.finding = finding;
        return this;
    }
}
