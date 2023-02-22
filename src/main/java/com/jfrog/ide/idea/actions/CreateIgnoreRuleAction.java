package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.jfrog.ide.idea.ui.configuration.JFrogGlobalConfiguration;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import static com.jfrog.ide.idea.ui.LocalComponentsTree.IGNORE_RULE_TOOL_TIP;
import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Create Ignore Rule button in the right-click menu of the issues table.
 *
 * @author yahavi
 **/
public class CreateIgnoreRuleAction extends AbstractAction {
    private final String ignoreRuleUrl;
    private final MouseEvent mouseEvent;

    public CreateIgnoreRuleAction(String ignoreRuleUrl, MouseEvent mouseEvent) {
        super("Create Vulnerability Ignore Rule", AllIcons.RunConfigurations.ShowIgnored);
        this.ignoreRuleUrl = ignoreRuleUrl;
        this.mouseEvent = mouseEvent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isBlank(this.ignoreRuleUrl)) {
            Balloon balloon = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(IGNORE_RULE_TOOL_TIP + "<br><a href=\"Configure it here.\"> Configure it here. </a>", MessageType.ERROR,
                            event -> {
                                if (event.getEventType() != ACTIVATED) {
                                    return;
                                }
                                ShowSettingsUtil.getInstance().showSettingsDialog(null, JFrogGlobalConfiguration.class, JFrogGlobalConfiguration::selectSettingsTab);
                            })
                    .setHideOnAction(true)
                    .setHideOnClickOutside(true)
                    .setHideOnLinkClick(true)
                    .setHideOnKeyOutside(true)
                    .setDialogMode(true)
                    .createBalloon();
            balloon.show(new RelativePoint(mouseEvent), Balloon.Position.above);
        } else {
            BrowserUtil.browse(ignoreRuleUrl);
        }
    }
}
