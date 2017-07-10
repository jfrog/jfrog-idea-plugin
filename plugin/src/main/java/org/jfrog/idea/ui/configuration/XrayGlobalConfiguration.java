package org.jfrog.idea.ui.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.messages.MessageBus;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.services.system.Version;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jfrog.idea.Events;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.configuration.XrayServerConfig;
import org.jfrog.idea.xray.utils.Utils;

import javax.swing.*;
import java.io.IOException;

import static org.jfrog.idea.xray.utils.Utils.MINIMAL_XRAY_VERSION_SUPPORTED;

/**
 * Created by romang on 1/29/17.
 */
public class XrayGlobalConfiguration implements Configurable, Configurable.NoScroll {

    private JPanel config;
    private JBTextField url;
    private JBTextField username;
    private JBPasswordField password;
    private JButton testConnectionButton;
    private JLabel connectionResults;
    private XrayServerConfig xrayConfig;

    public XrayGlobalConfiguration() {
        testConnectionButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                connectionResults.setText("Connecting to Xray...");
                config.validate();
                config.repaint();
                // use as a workaround to version not being username password validated
                Xray xrayClient = XrayClient.create(StringUtil.trim(url.getText()), StringUtil.trim(username.getText()), String.valueOf(password.getPassword()));
                xrayClient.binaryManagers().artifactoryConfigurations();
                Version xrayVersion = xrayClient.system().version();

                if (!Utils.isXrayVersionSupported(xrayVersion)) {
                    connectionResults.setText("ERROR: Unsupported Xray version: " + xrayVersion.getVersion() +
                            ", version " + MINIMAL_XRAY_VERSION_SUPPORTED + " or above is required.");
                } else {
                    connectionResults.setText("Successfully connected to Xray version: " + xrayVersion.getVersion());
                }
            } catch (IOException | IllegalArgumentException e1) {
                connectionResults.setText("Could not connect to Xray: " + e1.getMessage());
            }
            config.validate();
            config.repaint();
        }));
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
        xrayConfig = XrayServerConfig.newBuilder()
                .setUrl(url.getText())
                .setUsername(username.getText())
                .setPassword(String.valueOf(password.getPassword()))
                .build();

        return !xrayConfig.equals(GlobalSettings.getInstance().getXrayConfig());
    }

    @Override
    public void apply() throws ConfigurationException {
        GlobalSettings globalSettings = GlobalSettings.getInstance();
        globalSettings.setXrayConfig(xrayConfig);
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(Events.ON_CONFIGURATION_DETAILS_CHANGE).update();
        connectionResults.setText("");
    }

    @Override
    public void reset() {
        loadConfig();
    }

    @Override
    public void disposeUIResources() {

    }

    private void loadConfig() {
        url.getEmptyText().setText("Example: http://localhost:8000");
        connectionResults.setText("");

        xrayConfig = GlobalSettings.getInstance().getXrayConfig();
        if (xrayConfig != null) {
            url.setText(xrayConfig.getUrl());
            username.setText(xrayConfig.getUsername());
            password.setText(xrayConfig.getPassword());
        } else {
            url.setText("");
            username.setText("");
            password.setText("");
        }
    }

    private void createUIComponents() {
        xrayConfig = GlobalSettings.getInstance().getXrayConfig();
        url = new JBTextField();
        username = new JBTextField();
        password = new JBPasswordField();

        loadConfig();
    }
}
