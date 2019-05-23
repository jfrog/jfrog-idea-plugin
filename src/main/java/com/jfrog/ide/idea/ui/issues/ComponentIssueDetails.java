package com.jfrog.ide.idea.ui.issues;

import com.jfrog.ide.idea.ui.ComponentDetail;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.*;

/**
 * @author yahavi
 */
class ComponentIssueDetails extends ComponentDetail {

    private ComponentIssueDetails(DependenciesTree node) {
        super(node);
        Issue topIssue = node.getTopIssue();
        addText("Top Issue Severity:", StringUtils.capitalize(topIssue.getSeverity().toString()));
        addText("Top Issue Type:", StringUtils.capitalize(topIssue.getIssueType()));
        addText("Issues Count:", String.valueOf(node.getIssueCount()));
    }

    static void createIssuesDetailsView(JPanel panel, DependenciesTree node) {
        if (node == null || node.getGeneralInfo() == null) {
            createComponentInfoNotAvailablePanel(panel);
            return;
        }
        replaceAndUpdateUI(panel, new ComponentIssueDetails(node), BorderLayout.NORTH);
    }
}
