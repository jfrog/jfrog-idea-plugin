package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class ApplicableDetails implements Serializable {
    boolean isApplicable;
    Evidence[] evidence;
    String searchTarget;

    public ApplicableDetails(boolean isApplicable, Evidence[] evidence, String searchTarget) {
        this.isApplicable = isApplicable;
        this.evidence = evidence;
        this.searchTarget = searchTarget;
    }
}
