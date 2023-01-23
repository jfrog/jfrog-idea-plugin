package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Create Ignore Rule button in the right-click menu of the issues table.
 *
 * @author yahavi
 **/
public class CreateIgnoreRuleAction extends AbstractAction {
    private final String ignoreRuleUrl;

    public CreateIgnoreRuleAction(String ignoreRuleUrl) {
        super("Create Vulnerability Ignore Rule", AllIcons.RunConfigurations.ShowIgnored);
        this.ignoreRuleUrl = ignoreRuleUrl;
        this.setEnabled(isNotBlank(this.ignoreRuleUrl));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BrowserUtil.browse(ignoreRuleUrl);
    }
}
