package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class ScanConfig {
    @JsonProperty("type")
    private SourceCodeScanType scanType;
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
    @JsonProperty("excluded-rules")
    private List<String> excludedRules;

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
        this.excludedRules = builder.excludedRules;
    }

    @SuppressWarnings("unused")
    public SourceCodeScanType getScanType() {
        return scanType;
    }

    @SuppressWarnings("unused")
    public void setScanType(SourceCodeScanType scanType) {
        this.scanType = scanType;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @SuppressWarnings("unused")
    public void setRoots(List<String> roots) {
        this.roots = roots;
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
        private SourceCodeScanType scanType;
        private String language;
        private List<String> roots;
        private String output;
        private Boolean grepDisable;
        private List<String> cves;
        private List<String> skippedFolders;
        private List<String> excludedRules;

        public Builder() {
            roots = new ArrayList<>();
            cves = new ArrayList<>();
            skippedFolders = new ArrayList<>();
            excludedRules = new ArrayList<>();
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder scanType(SourceCodeScanType scanType) {
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

        @SuppressWarnings("unused")
        public Builder excludedRules(List<String> excludedRules) {
            this.excludedRules = excludedRules;
            return this;
        }

        public ScanConfig Build() {
            return new ScanConfig(this);
        }
    }
}