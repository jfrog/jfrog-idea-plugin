package com.jfrog.ide.idea.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents all supported file descriptor types.
 */
public enum Descriptor {
    MAVEN("pom.xml"), GRADLE_KOTLIN("build.gradle.kts"), GRADLE_GROOVY("build.gradle"), NPM("package.json"), GO("go.mod");

    private final String fileName;

    Descriptor(String fileName) {
        this.fileName = fileName;
    }

    public static Descriptor fromFileName(String fileName) {
        for (Descriptor descriptor : Descriptor.values()) {
            if (StringUtils.equals(descriptor.getFileName(), fileName)) {
                return descriptor;
            }
        }
        return null;
    }

    public String getFileName() {
        return fileName;
    }
}
