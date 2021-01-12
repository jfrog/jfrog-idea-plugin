package com.jfrog.ide.idea.ui.configuration;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;

import javax.swing.*;
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
     * Migrate {@link com.jfrog.ide.idea.configuration.XrayServerConfigImpl} to {@link ServerConfigImpl}.
     * Remove password from PasswordSafe and
     * @param serverConfig - The old server config to migrate
     */
    public static void migrateXrayConfigToPlatformConfig(ServerConfigImpl serverConfig) {
        Credentials credentials = serverConfig.getLegacyCredentialsFromPasswordSafe();
        if (credentials != null) {
            // Remove credentials from PasswordSafe to allow migration to the new PasswordSafe keys
            serverConfig.setUsername(credentials.getUserName());
            serverConfig.setPassword(credentials.getPasswordAsString());
            serverConfig.removeLegacyCredentialsFromPasswordSafe();
        }
        String xrayUrl = removeEnd(serverConfig.getXrayUrl(), "/");
        if (endsWith(xrayUrl, "/xray")) {
            String platformUrl = removeEnd(xrayUrl, "/xray");
            serverConfig.setUrl(platformUrl);
            serverConfig.setArtifactoryUrl(platformUrl + "/artifactory");
        } else {
            serverConfig.setUrl("");
        }
    }
}
