package org.jfrog.idea.xray.persistency.types;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by romang on 4/16/17.
 */
public enum Severity {
    /**
     * New severities.
     *
     * @since Xray 2.5
     */
    Normal("Scanned - No Issues", 0),
    Pending("Pending Scan", 1),
    Unknown("Unknown", 2),
    Information("Information", 3),
    Low("Low", 4),
    Medium("Medium", 5),
    High("High", 6),

    /**
     * Old severities.
     *
     * @deprecated since Xray 2.5
     */
    Minor("Low", 4),
    Major("Medium", 5),
    Critical("High", 6);

    public static final Set<Severity> NEW_SEVERITIES = Sets.newHashSet(Normal, Pending, Unknown, Information, Low, Medium, High);
    private String severityName;
    private int ordinal;

    Severity(String severityName, int ordinal) {
        this.severityName = severityName;
        this.ordinal = ordinal;
    }

    public String getSeverityName() {
        return this.severityName;
    }

    public int getOrdinal() {
        return this.ordinal;
    }

    public boolean isHigherThan(Severity other) {
        return this.ordinal > other.getOrdinal();
    }

    public static Severity fromString(String inputSeverity) {
        for (Severity severity : NEW_SEVERITIES) {
            if (severity.getSeverityName().equals(inputSeverity)) {
                return severity;
            }
        }
        // Backward compatibility
        switch (inputSeverity) {
            case "Critical":
                return High;
            case "Major":
                return Medium;
            case "Minor":
                return Low;
            case "Normal":
                return Normal;
        }
        throw new IllegalArgumentException("Severity " + inputSeverity + " doesn't exist");
    }

}