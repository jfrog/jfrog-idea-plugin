package com.jfrog.ide.idea.ui;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.*;

import static com.jfrog.ide.idea.ui.utils.ComponentUtils.replaceAndUpdateUI;

/**
 * @author yahavi
 */
public class ComponentIssueDetails extends ComponentDetails {

    private ComponentIssueDetails(DependencyTree node) {
        super(node);
        Issue topIssue = node.getTopIssue();
        addText("Top Issue Severity", StringUtils.capitalize(topIssue.getSeverity().toString()));
    }

    static void createIssuesDetailsView(JPanel panel, DependencyTree node) {
        if (node == null || node.getGeneralInfo() == null) {
            createComponentInfoNotAvailablePanel(panel);
            return;
        }
        replaceAndUpdateUI(panel, new ComponentIssueDetails(node), BorderLayout.NORTH);
    }
}
