package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Create Ignore Rule button in the right-click menu of the issues table.
 *
 * @author yahavi
 **/
public class CreateIgnoreRuleAction extends AbstractAction {
    private final Issue selectedIssue;

    public CreateIgnoreRuleAction(Issue selectedIssue) {
        super("Create Ignore Rule", AllIcons.RunConfigurations.ShowIgnored);
        this.selectedIssue = selectedIssue;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BrowserUtil.browse(selectedIssue.getIgnoreRuleUrl());
    }
}
