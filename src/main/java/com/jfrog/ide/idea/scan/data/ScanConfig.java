package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"type", "language", "roots", "output"})
public class ScanConfig {
    @JsonProperty("type")
    private String scanType;
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

    @SuppressWarnings("UnusedReturnValue")
    public String getScanType() {
        return scanType;
    }

    @SuppressWarnings("UnusedReturnValue")
    public void setScanType(String scanType) {
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

    @SuppressWarnings("UnusedReturnValue")

    public void setRoots(List<String> roots) {
        this.roots = roots;
    }

    public String getOutput() {
        return output;
    }

    @SuppressWarnings("UnusedReturnValue")

    public void setOutput(String output) {
        this.output = output;
    }


    public static class Builder {
        private String scanType;
        private String language;
        private List<String> roots;
        private String output;
        private Boolean grepDisable;
        private List<String> cves;
        private List<String> skippedFolders;

        public Builder() {
        }

        public Builder scanType(String scanType) {
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

        public Builder output(String output) {
            this.output = output;
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder grepDisable(Boolean grepDisable) {
            this.grepDisable = grepDisable;
            return this;
        }

        public Builder cves(List<String> cves) {
            this.cves = cves;
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder skippedFolders(List<String> skippedFolders) {
            this.skippedFolders = skippedFolders;
            return this;
        }

        public ScanConfig Build() {
            return new ScanConfig(this);
        }

    }
}