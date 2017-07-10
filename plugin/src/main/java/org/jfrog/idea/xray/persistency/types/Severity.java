package org.jfrog.idea.xray.persistency.types;

/**
 * Created by romang on 4/16/17.
 */
public enum Severity {
    critical(0), major(1), minor(2), unknown(3);
    private final int severity;

    Severity(int i) {
        severity = i;
    }

    int getValue() {
        return severity;
    }
}