package org.jfrog.idea.xray.persistency.types;

/**
 * Created by romang on 4/16/17.
 */
public enum Severity {
    Normal("Scanned - No Issues", 0),
    Pending("Pending Scan", 1),
    Unknown("Unknown", 2),
    Information("Information", 3),
    Low("Low", 4),
    Minor("Low", 4),
    Medium("Medium", 5),
    Major("Medium", 5),
    High("High", 6),
    Critical("High", 6);

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
        for (Severity severity : Severity.values()) {
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