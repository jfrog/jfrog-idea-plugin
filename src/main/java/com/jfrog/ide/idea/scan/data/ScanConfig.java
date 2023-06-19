package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.ScanType;

import java.util.List;

public class ScanConfig {
    @JsonProperty("type")
    private ScanType scanType;
    @JsonProperty("language")
    private String language;
    @JsonProperty("roots")
    private List<String> roots;
    @JsonProperty("output")
    private String output;
    @JsonProperty("grep-disable")
    private Boolean grepDisable;

    @JsonProperty("cve-whitelist")
    private List<String> cves;

    @JsonProperty("skipped-folders")
    private List<String> skippedFolders;

    @SuppressWarnings("unused")
    ScanConfig() {
    }

    ScanConfig(Builder builder) {
        this.scanType = builder.scanType;
        this.language = builder.language;
        this.roots = builder.roots;
        this.output = builder.output;
        this.cves = builder.cves;
        this.grepDisable = builder.grepDisable;
        this.skippedFolders = builder.skippedFolders;
    }

    @SuppressWarnings("unused")
    public ScanType getScanType() {
        return scanType;
    }

    @SuppressWarnings("unused")
    public void setScanType(ScanType scanType) {
        this.scanType = scanType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getRoots() {
        return roots;
    }

    @SuppressWarnings("unused")
    public void setRoots(List<String> roots) {
        this.roots = roots;
    }

    public String getOutput() {
        return output;
    }

    @SuppressWarnings("unused")
    public void setOutput(String output) {
        this.output = output;
    }

    @SuppressWarnings("unused")
    public Boolean getGrepDisable() {
        return grepDisable;
    }

    @SuppressWarnings("unused")
    public void setGrepDisable(Boolean grepDisable) {
        this.grepDisable = grepDisable;
    }

    @SuppressWarnings("unused")
    public List<String> getCves() {
        return cves;
    }

    @SuppressWarnings("unused")
    public void setCves(List<String> cves) {
        this.cves = cves;
    }

    @SuppressWarnings("unused")
    public List<String> getSkippedFolders() {
        return skippedFolders;
    }

    @SuppressWarnings("unused")
    public void setSkippedFolders(List<String> skippedFolders) {
        this.skippedFolders = skippedFolders;
    }


    public static class Builder {
        private ScanType scanType;
        private String language;
        private List<String> roots;
        private String output;
        private Boolean grepDisable;
        private List<String> cves;
        private List<String> skippedFolders;

        public Builder() {
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder scanType(ScanType scanType) {
            this.scanType = scanType;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder roots(List<String> roots) {
            this.roots = roots;
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder output(String output) {
            this.output = output;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder grepDisable(Boolean grepDisable) {
            this.grepDisable = grepDisable;
            return this;
        }

        public Builder cves(List<String> cves) {
            this.cves = cves;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder skippedFolders(List<String> skippedFolders) {
            this.skippedFolders = skippedFolders;
            return this;
        }

        public ScanConfig Build() {
            return new ScanConfig(this);
        }

    }
}