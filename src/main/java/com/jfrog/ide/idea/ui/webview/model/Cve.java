package com.jfrog.ide.idea.ui.webview.model;

public class Cve {
    private final String id;
    private final String cvssV2Score;
    private final String cvssV2Vector;
    private final String cvssV3Score;
    private final String cvssV3Vector;
    private final ApplicableDetails applicableData;

    public Cve(String id, String cvssV2Score, String cvssV2Vector, String cvssV3Score, String cvssV3Vector, ApplicableDetails applicableData) {
        this.id = id;
        this.cvssV2Score = cvssV2Score;
        this.cvssV2Vector = cvssV2Vector;
        this.cvssV3Score = cvssV3Score;
        this.cvssV3Vector = cvssV3Vector;
        this.applicableData = applicableData;
    }

    @SuppressWarnings("unused")
    public String getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public String getCvssV2Vector() {
        return cvssV2Vector;
    }

    @SuppressWarnings("unused")
    public String getCvssV2Score() {
        return cvssV2Score;
    }

    @SuppressWarnings("unused")
    public String getCvssV3Score() {
        return cvssV3Score;
    }

    @SuppressWarnings("unused")
    public String getCvssV3Vector() {
        return cvssV3Vector;
    }

    @SuppressWarnings("unused")
    public ApplicableDetails getApplicableData() {
        return applicableData;
    }
}
