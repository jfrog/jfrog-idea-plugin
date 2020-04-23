package com.jfrog.ide.idea.ui.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.messages.MessageBus;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.XrayServerConfigImpl;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jfrog.client.util.KeyStoreProviderException;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.regex.PatternSyntaxException;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.*;
import static com.jfrog.ide.idea.configuration.XrayServerConfigImpl.DEFAULT_EXCLUSIONS;

/**
 * Created by romang on 1/29/17.
 */
public class XrayGlobalConfiguration implements Configurable, Configurable.NoScroll {

    private static final String USER_AGENT = "jfrog-idea-plugin/" + XrayGlobalConfiguration.class.getPackage().getImplementationVersion();

    private XrayServerConfigImpl xrayConfig;
    private JButton testConnectionButton;
    private JBPasswordField password;
    private JLabel connectionResults;
    private JBTextField excludedPaths;
    private JBTextField username;
    private JBTextField url;
    private JPanel config;
    private JBCheckBox connectionDetailsFromEnv;

    public XrayGlobalConfiguration() {
        testConnectionButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                connectionResults.setText("Connecting to Xray...");
                config.validate();
                config.repaint();
                // use as a workaround to version not being username password validated
                Xray xrayClient = createXrayClient();
                Version xrayVersion = xrayClient.system().version();

                // Check version
                if (!isXrayVersionSupported(xrayVersion)) {
                    connectionResults.setText(Results.unsupported(xrayVersion));
                    return;
                }

                // Check permissions
                Pair<Boolean, String> testComponentPermissionRes = testComponentPermission(xrayClient);
                if (!testComponentPermissionRes.getLeft()) {
                    throw new IOException(testComponentPermissionRes.getRight());
                }

                connectionResults.setText(Results.success(xrayVersion));

            } catch (IOException | KeyStoreProviderException exception) {
                connectionResults.setText(Results.error(exception));
            }
        }));
        connectionDetailsFromEnv.addItemListener(e -> {
            JBCheckBox cb = (JBCheckBox) e.getSource();
            if (cb.isSelected()) {
                username.setEnabled(false);
                url.setEnabled(false);
                password.setEnabled(false);
                xrayConfig.readConnectionDetailsFromEnv();
                updateConnectionDetailsTextFields();
            } else {
                username.setEnabled(true);
                url.setEnabled(true);
                password.setEnabled(true);
            }
        });
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "JFrog Xray Configuration";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "Setup page for JFrog Xray URL and credentials";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return config;
    }

    @Override
    public boolean isModified() {
        xrayConfig = XrayServerConfigImpl.newBuilder()
                .setUrl(url.getText())
                .setUsername(username.getText())
                .setPassword(String.valueOf(password.getPassword()))
                .setExcludedPaths(excludedPaths.getText())
                .setConnectionDetailsFromEnv(connectionDetailsFromEnv.isSelected())
                .build();

        return !xrayConfig.equals(GlobalSettings.getInstance().getXrayConfig());
    }

    @Override
    public void apply() {
        GlobalSettings globalSettings = GlobalSettings.getInstance();
        globalSettings.updateConfig(xrayConfig);
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE).update();
        connectionResults.setText("");
        loadConfig();
    }

    @Override
    public void reset() {
        loadConfig();
    }

    @Override
    public void disposeUIResources() {

    }

    private Xray createXrayClient() throws KeyStoreProviderException {
        // use as a workaround to version not being username password validated
        String urlStr = StringUtil.trim(url.getText());
        return XrayClient.create(urlStr,
                StringUtil.trim(username.getText()),
                String.valueOf(password.getPassword()),
                USER_AGENT,
                xrayConfig.isNoHostVerification(),
                xrayConfig.getKeyStoreProvider(),
                xrayConfig.getProxyConfForTargetUrl(urlStr));
    }

    private void loadConfig() {
        url.getEmptyText().setText("Example: http://localhost:8000");
        excludedPaths.setInputVerifier(new ExclusionsVerifier());
        connectionResults.setText("");

        xrayConfig = GlobalSettings.getInstance().getXrayConfig();
        if (xrayConfig != null) {
            updateConnectionDetailsTextFields();
            excludedPaths.setText(xrayConfig.getExcludedPaths());
            connectionDetailsFromEnv.setSelected(xrayConfig.isConnectionDetailsFromEnv());
        } else {
            url.setText("");
            username.setText("");
            password.setText("");
            excludedPaths.setText(DEFAULT_EXCLUSIONS);
            connectionDetailsFromEnv.setSelected(false);
        }
    }

    private void updateConnectionDetailsTextFields() {
        url.setText(xrayConfig.getUrl());
        username.setText(xrayConfig.getUsername());
        password.setText(xrayConfig.getPassword());
    }

    @SuppressWarnings("BoundFieldAssignment")
    private void createUIComponents() {
        xrayConfig = GlobalSettings.getInstance().getXrayConfig();
        url = new JBTextField();
        username = new JBTextField();
        password = new JBPasswordField();
        excludedPaths = new JBTextField();
        connectionDetailsFromEnv = new JBCheckBox();

        loadConfig();
    }

    private class ExclusionsVerifier extends InputVerifier {
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            if (verify(input)) {
                return true;
            }
            excludedPaths.setText(DEFAULT_EXCLUSIONS);
            return false;
        }

        @Override
        public boolean verify(JComponent input) {
            if (StringUtils.isBlank(excludedPaths.getText())) {
                return false;
            }
            try {
                FileSystems.getDefault().getPathMatcher("glob:" + excludedPaths.getText());
            } catch (PatternSyntaxException e) {
                return false;
            }
            return true;
        }
    }
}
