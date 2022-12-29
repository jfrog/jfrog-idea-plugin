package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class Cve implements Serializable {
    String id;
    String cvssV2Score;
    String cvssV2Vector;
    String cvssV3Score;
    String cvssV3Vector;
    ApplicableDetails applicableData;

    public Cve() {
    }

    public Cve(String id, String cvssV2Score, String cvssV2Vector, String cvssV3Score, String cvssV3Vector, ApplicableDetails applicableData) {
        this.id = id;
        this.cvssV2Score = cvssV2Score;
        this.cvssV2Vector = cvssV2Vector;
        this.cvssV3Score = cvssV3Score;
        this.cvssV3Vector = cvssV3Vector;
        this.applicableData = applicableData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getCvssV2Vector() {
        return cvssV2Vector;
    }

    public void setCvssV2Vector(String cvssV2Vector) {
        this.cvssV2Vector = cvssV2Vector;
    }

    public String getCvssV2Score() {
        return cvssV2Score;
    }

    public void setCvssV2Score(String cvssV2Score) {
        this.cvssV2Score = cvssV2Score;
    }

    public String getCvssV3Score() {
        return cvssV3Score;
    }

    public void setCvssV3Score(String cvssV3Score) {
        this.cvssV3Score = cvssV3Score;
    }

    public String getCvssV3Vector() {
        return cvssV3Vector;
    }

    public void setCvssV3Vector(String cvssV3Vector) {
        this.cvssV3Vector = cvssV3Vector;
    }

    public ApplicableDetails getApplicableData() {
        return applicableData;
    }

    public void setApplicableData(ApplicableDetails applicableData) {
        this.applicableData = applicableData;
    }
}
