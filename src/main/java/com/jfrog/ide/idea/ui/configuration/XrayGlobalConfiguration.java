package com.jfrog.ide.idea.ui.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.messages.MessageBus;
import com.jfrog.ide.idea.Events;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.XrayServerConfigImpl;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.XrayClient;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static com.jfrog.ide.common.utils.XrayConnectionUtils.*;

/**
 * Created by romang on 1/29/17.
 */
public class XrayGlobalConfiguration implements Configurable, Configurable.NoScroll {

    private static final String USER_AGENT = "jfrog-idea-plugin/" + XrayGlobalConfiguration.class.getPackage().getImplementationVersion();

    private XrayServerConfigImpl xrayConfig;
    private JButton testConnectionButton;
    private JBPasswordField password;
    private JLabel connectionResults;
    private JBTextField username;
    private JBTextField url;
    private JPanel config;

    public XrayGlobalConfiguration() {
        testConnectionButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                connectionResults.setText("Connecting to Xray...");
                config.validate();
                config.repaint();
                // use as a workaround to version not being username password validated
                Xray xrayClient = XrayClient.create(StringUtil.trim(url.getText()), StringUtil.trim(username.getText()), String.valueOf(password.getPassword()), USER_AGENT, xrayConfig.getProxyConfig());
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

            } catch (IOException exception) {
                connectionResults.setText(Results.error(exception));
            }
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
        xrayConfig = XrayServerConfigImpl.newBuilder()
                .setUrl(url.getText())
                .setUsername(username.getText())
                .setPassword(String.valueOf(password.getPassword()))
                .build();

        return !xrayConfig.equals(GlobalSettings.getInstance().getXrayConfig());
    }

    @Override
    public void apply() {
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

    @SuppressWarnings("BoundFieldAssignment")
    private void createUIComponents() {
        xrayConfig = GlobalSettings.getInstance().getXrayConfig();
        url = new JBTextField();
        username = new JBTextField();
        password = new JBPasswordField();

        loadConfig();
    }
}
