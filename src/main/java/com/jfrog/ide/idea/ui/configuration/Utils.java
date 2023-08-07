package com.jfrog.ide.idea.ui.configuration;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.Time;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author yahavi
 **/
public class Utils {

    /**
     * Set active background for hovering.
     *
     * @param label - The label to set
     */
    public static void setActiveForegroundColor(JLabel label) {
        label.setForeground(UIUtil.isUnderDarcula() ? UIUtil.getActiveTextColor() : UIUtil.getTextAreaForeground());
    }

    /**
     * Set inactive background for hovering.
     *
     * @param label - The label to set
     */
    public static void setInactiveForegroundColor(JLabel label) {
        label.setForeground(UIUtil.isUnderDarcula() ? UIUtil.getHeaderInactiveColor() : UIUtil.getInactiveTextColor());
    }

    /**
     * Clear text for multiple text fields.
     *
     * @param textFields - The text fields
     */
    public static void clearText(JTextField... textFields) {
        Arrays.stream(textFields).forEach(textField -> textField.setText(""));
    }

    /**
     * Get credentials from PasswordSafe if exist. Otherwise, null.
     *
     * @param subsystem - The subsystem key in the PasswordSafe, typically com.jfrog.idea
     * @param key       - The key inside the plugin settings, typically password
     * @return credentials from PasswordSafe if exist. Otherwise, null.
     */
    public static Credentials retrieveCredentialsFromPasswordSafe(String subsystem, String key) {
        if (isBlank(key)) {
            return null;
        }
        try {
            return PasswordSafe.getInstance().get(createCredentialAttributes(subsystem, key));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Store credentials in PasswordSafe.
     *
     * @param subsystem   - The subsystem key in the PasswordSafe, typically com.jfrog.idea
     * @param key         - The key inside the plugin settings, typically password
     * @param credentials - The credentials to store
     */
    public static void storeCredentialsInPasswordSafe(String subsystem, String key, Credentials credentials) {
        if (isBlank(key)) {
            return;
        }
        PasswordSafe.getInstance().set(createCredentialAttributes(subsystem, key), credentials);
    }

    /**
     * Remove credentials from PasswordSafe.
     *
     * @param subsystem - The subsystem key in the PasswordSafe, typically com.jfrog.idea
     * @param key       - The key inside the plugin settings, typically password
     */
    public static void removeCredentialsInPasswordSafe(String subsystem, String key) {
        storeCredentialsInPasswordSafe(subsystem, key, null);
    }

    /**
     * Create credentials attributes to use as the key in the PasswordSafe.
     *
     * @param subsystem - The subsystem key in the PasswordSafe, typically com.jfrog.idea
     * @param key       - The key inside the plugin settings, typically password
     * @return the new credentials attributes.
     */
    public static CredentialAttributes createCredentialAttributes(String subsystem, String key) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(subsystem, key));
    }

    /**
     * Set the input HyperlinkLabel
     *
     * @param label - The hyperlink label to set
     * @param text  - The text
     * @param link  - The link
     */
    @SuppressWarnings("UnstableApiUsage")
    public static void initHyperlinkLabel(HyperlinkLabel label, String text, String link) {
        label.setTextWithHyperlink("    " + text);
        label.addHyperlinkListener(l -> BrowserUtil.browse(link));
        label.setForeground(UIUtil.getInactiveTextColor());
    }

    /**
     * Create the connection results balloon.
     *
     * @param message   - Connection results text
     * @param component - The component to show the results on
     */
    public static void createConnectionResultsBalloon(String message, JComponent component) {
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setFadeoutTime(Time.SECOND * 10)
                .setDialogMode(true)
                .setTitle("Connection Testing")
                .createBalloon().showInCenterOf(component);
    }

    /**
     * Add a temporary red border to a text component. The border disappear after gaining the focus.
     *
     * @param component - The text component
     */
    public static void addRedBorder(JTextComponent component) {
        component.setBorder(BorderFactory.createLineBorder(UIUtil.getErrorForeground()));
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                component.setBorder(UIUtil.getTextFieldBorder());
                component.removeFocusListener(this);
            }
        });
    }
}
