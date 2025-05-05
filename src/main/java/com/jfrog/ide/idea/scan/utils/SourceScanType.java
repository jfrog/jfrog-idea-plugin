package com.jfrog.ide.idea.scan.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;

//TODO: Delete this enum and use the one in the IDE common once the plugin starts using the CLI
public enum SourceScanType {
    CONTEXTUAL("analyze-applicability"),
    SECRETS("secrets-scan"),
    IAC("iac-scan-modules"),
    SAST("sast"),
    SCA("JFrog Xray Scanner");

    private final String scannerName;

    @JsonCreator
    private SourceScanType(String scannerName) {
        this.scannerName = scannerName;
    }

    @JsonValue
    public String getScannerName() {
        return this.scannerName;
    }

    public static SourceCodeScanType toSourceCodeScanType(SourceScanType sourceScanType) {
        try {
            return SourceCodeScanType.valueOf(sourceScanType.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No matching SourceCodeScanType for SourceScanType: " + sourceScanType.name(), e);
        }
    }
}
