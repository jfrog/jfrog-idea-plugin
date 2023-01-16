package com.jfrog.ide.idea.ui.configuration;

import com.google.common.collect.Lists;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.*;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.components.ConnectionResultsGesture;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.ci.Utils.createAqlForBuildArtifacts;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.*;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.DEFAULT_EXCLUSIONS;
import static com.jfrog.ide.idea.ui.configuration.Utils.clearText;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * Created by romang on 1/29/17.
 */
public class JFrogGlobalConfiguration implements Configurable, Configurable.NoScroll {

    public static final String USER_AGENT = "jfrog-idea-plugin/" + JFrogGlobalConfiguration.class.getPackage().getImplementationVersion();

    private JPanel connectionDetails;
    private JPanel settings;
    private JPanel advanced;

    private ServerConfigImpl serverConfig;
    private JButton testConnectionButton;
    private JBPasswordField password;
    private JBLabel connectionResults;
    private JBTextField excludedPaths;
    private JBTextField project;
    private JBTextField watches;
    private JBTextField username;
    private JBTextField platformUrl;
    private JBCheckBox connectionDetailsFromEnv;
    private ConnectionRetriesSpinner connectionRetries;
    private ConnectionTimeoutSpinner connectionTimeout;

    private JBTextField xrayUrl;
    private JBTextField artifactoryUrl;
    private JCheckBox setRtAndXraySeparately;
    private JBLabel xrayConnectionResults;
    private JBLabel artifactoryConnectionResults;
    private ConnectionResultsGesture connectionResultsGesture;
    private ConnectionResultsGesture artifactoryConnectionResultsGesture;
    private HyperlinkLabel projectInstructions;
    private HyperlinkLabel policyInstructions;
    private HyperlinkLabel watchInstructions;
    private JBPasswordField accessToken;

    // Authentication types
    private JRadioButton usernamePasswordRadioButton;
    private JRadioButton accessTokenRadioButton;

    // Scan policies
    private JRadioButton allVulnerabilitiesRadioButton;
    private JRadioButton accordingToProjectRadioButton;
    private JRadioButton accordingToWatchesRadioButton;
    private JButton defaultValuesButton;

    public JFrogGlobalConfiguration() {
        initUrls();
        initTestConnection();
        initConnectionDetailsFromEnv();
        initAuthenticationMethod();
        initPolicy();
        initLinks();
        initDefaultValuesButton();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.add("Connection Details", connectionDetails);
        tabbedPane.add("Settings", settings);
        tabbedPane.add("Advanced", advanced);
        return tabbedPane;
    }

    private void initUrls() {
        setRtAndXraySeparately.addActionListener(listener -> {
            boolean selected = ((AbstractButton) listener.getSource()).isSelected();
            xrayUrl.setEnabled(selected);
            artifactoryUrl.setEnabled(selected);
            if (!selected) {
                resolveXrayAndArtifactoryUrls();
            }
        });
        platformUrl.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!setRtAndXraySeparately.isSelected()) {
                    resolveXrayAndArtifactoryUrls();
                }
            }
        });
        xrayUrl.setEnabled(false);
        artifactoryUrl.setEnabled(false);
    }

    private void initTestConnection() {
        connectionResultsGesture = new ConnectionResultsGesture(xrayConnectionResults);
        artifactoryConnectionResultsGesture = new ConnectionResultsGesture(artifactoryConnectionResults);
        testConnectionButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (!setRtAndXraySeparately.isSelected()) {
                resolveXrayAndArtifactoryUrls();
            }
            List<String> results = new ArrayList<>();
            addIgnoreNull(results, checkXrayConnection());
            addIgnoreNull(results, checkArtifactoryConnection());
            setConnectionResults(String.join("<br/>", results));
        }));
    }

    private void initDefaultValuesButton() {

        defaultValuesButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            excludedPaths.setText(DEFAULT_EXCLUSIONS);
            connectionRetries.setValue(ConnectionRetriesSpinner.RANGE.initial);
            connectionTimeout.setValue(ConnectionTimeoutSpinner.RANGE.initial);
        }));
    }

    private void resolveXrayAndArtifactoryUrls() {
        String platformUrlStr = platformUrl.getText();
        if (isBlank(platformUrlStr)) {
            if (!setRtAndXraySeparately.isSelected()) {
                clearText(xrayUrl, artifactoryUrl);
            }
            return;
        }
        platformUrlStr = removeEnd(platformUrlStr, "/");
        xrayUrl.setText(platformUrlStr + "/xray");
        artifactoryUrl.setText(platformUrlStr + "/artifactory");
    }

    private String checkXrayConnection() {
        if (isBlank(xrayUrl.getText())) {
            return null;
        }
        try {
            Xray xrayClient = createXrayClient();

            setConnectionResults("Connecting to Xray...");
            connectionDetails.validate();
            connectionDetails.repaint();
            Version xrayVersion = xrayClient.system().version();

            // Check version
            if (!isSupportedInXrayVersion(xrayVersion)) {
                connectionResultsGesture.setFailure(Results.unsupported(xrayVersion));
                return Results.unsupported(xrayVersion);
            }

            // Check permissions
            Pair<Boolean, String> testComponentPermissionRes = testComponentPermission(xrayClient);
            if (!testComponentPermissionRes.getLeft()) {
                throw new IOException(testComponentPermissionRes.getRight());
            }

            connectionResultsGesture.setSuccess();
            return Results.success(xrayVersion);
        } catch (IOException exception) {
            connectionResultsGesture.setFailure(ExceptionUtils.getRootCauseMessage(exception));
            return "Could not connect to JFrog Xray.";
        }
    }

    private String checkArtifactoryConnection() {
        if (isBlank(artifactoryUrl.getText())) {
            return null;
        }
        try (ArtifactoryManager artifactoryManager = createArtifactoryManagerBuilder().build()) {
            setConnectionResults("Connecting to Artifactory...");
            connectionDetails.validate();
            connectionDetails.repaint();

            // Check connection.
            // This command will throw an exception if there is a connection or credentials issue.
            artifactoryManager.searchArtifactsByAql(createAqlForBuildArtifacts("*", "artifactory-build-info"));

            artifactoryConnectionResultsGesture.setSuccess();
            return "Successfully connected to Artifactory version: " + artifactoryManager.getVersion();
        } catch (Exception exception) {
            artifactoryConnectionResultsGesture.setFailure(ExceptionUtils.getRootCauseMessage(exception));
            return "Could not connect to JFrog Artifactory.";
        }
    }

    private void initConnectionDetailsFromEnv() {
        List<JComponent> effectedComponents = Lists.newArrayList(setRtAndXraySeparately, platformUrl, username, password, accessToken,
                accessTokenRadioButton, usernamePasswordRadioButton);
        connectionDetailsFromEnv.addItemListener(e -> {
            JBCheckBox cb = (JBCheckBox) e.getSource();
            if (cb.isSelected()) {
                xrayUrl.setEnabled(false);
                artifactoryUrl.setEnabled(false);
                effectedComponents.forEach(field -> field.setEnabled(false));
                serverConfig.readConnectionDetailsFromEnv();
                updateConnectionDetailsTextFields();
            } else {
                effectedComponents.forEach(field -> field.setEnabled(true));
                initAuthMethodSelection();
                // Restore connection details if original settings were inserted manually.
                // This will prevent losing data after checking/unchecking the checkbox.
                ServerConfigImpl oldConfig = GlobalSettings.getInstance().getServerConfig();
                if (oldConfig != null && !oldConfig.isConnectionDetailsFromEnv()) {
                    reset();
                }
            }
        });
    }

    private void initAuthenticationMethod() {
        initAuthMethodSelection();
        accessTokenRadioButton.addItemListener(e -> {
            JRadioButton accessTokenButton = (JRadioButton) e.getSource();
            ServerConfigImpl oldConfig = GlobalSettings.getInstance().getServerConfig();

            if (accessTokenButton.isSelected()) {
                accessToken.setEnabled(true);
                username.setText("");
                password.setText("");
                username.setEnabled(false);
                password.setEnabled(false);
                // Restore connection details if original settings were inserted manually.
                // This will prevent losing data after checking/unchecking the checkbox.
                if (oldConfig != null && isNotBlank(oldConfig.getAccessToken())) {
                    reset();
                }
            } else {
                username.setEnabled(true);
                password.setEnabled(true);
                accessToken.setText("");
                accessToken.setEnabled(false);
                // Restore connection details if original settings were inserted manually.
                // This will prevent losing data after checking/unchecking the checkbox.
                if (oldConfig != null && isNoneBlank(oldConfig.getUsername(), oldConfig.getPassword())) {
                    reset();
                }
            }
        });
    }

    private void initAuthMethodSelection() {
        boolean isAccessMode = isNotBlank(new String(accessToken.getPassword()));
        usernamePasswordRadioButton.setSelected(!isAccessMode);
        accessTokenRadioButton.setSelected(isAccessMode);
        accessToken.setEnabled(isAccessMode);
        username.setEnabled(!isAccessMode);
        password.setEnabled(!isAccessMode);
    }

    private void initPolicy() {
        accordingToWatchesRadioButton.addChangeListener(e -> {
            if (accordingToWatchesRadioButton.isSelected()) {
                watches.setEnabled(true);
                watches.setText(serverConfig.getWatches());
            } else {
                watches.setEnabled(false);
            }
        });
    }

    private void initLinks() {
        initHyperlink(projectInstructions, "Create a <hyperlink>JFrog Project</hyperlink>, or obtain the relevant JFrog Project key.", "https://www.jfrog.com/confluence/display/JFROG/Projects");
        initHyperlink(policyInstructions, "Create a <hyperlink>Policy</hyperlink> on JFrog Xray.", "https://www.jfrog.com/confluence/display/JFROG/Creating+Xray+Policies+and+Rules");
        initHyperlink(watchInstructions, "Create a <hyperlink>Watch</hyperlink> on JFrog Xray and assign your Policy and Project as resources to it.", "https://www.jfrog.com/confluence/display/JFROG/Configuring+Xray+Watches");
    }

    @SuppressWarnings("UnstableApiUsage")
    private void initHyperlink(HyperlinkLabel label, String text, String link) {
        label.setTextWithHyperlink("    " + text);
        label.addHyperlinkListener(l -> BrowserUtil.browse(link));
        label.setForeground(UIUtil.getInactiveTextColor());
    }

    private void setConnectionResults(String results) {
        if (results == null) {
            return;
        }
        connectionResults.setText("<html>" + results + "</html>");
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "JFrog Configuration";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "Setup page for JFrog Xray and Artifactory connection details.";
    }

    @Override
    public boolean isModified() {
        ServerConfig.PolicyType policyType = ServerConfig.PolicyType.VULNERABILITIES;
        if (accordingToProjectRadioButton.isSelected()) {
            policyType = ServerConfig.PolicyType.PROJECT;
        } else if (accordingToWatchesRadioButton.isSelected()) {
            policyType = ServerConfig.PolicyType.WATCHES;
        }
        serverConfig = new ServerConfigImpl.Builder()
                .setUrl(platformUrl.getText())
                .setArtifactoryUrl(artifactoryUrl.getText())
                .setXrayUrl(xrayUrl.getText())
                .setUsername(username.getText())
                .setPassword(String.valueOf(password.getPassword()))
                .setAccessToken(String.valueOf(accessToken.getPassword()))
                .setExcludedPaths(excludedPaths.getText())
                .setPolicyType(policyType)
                .setProject(project.getText())
                .setWatches(watches.getText())
                .setConnectionDetailsFromEnv(connectionDetailsFromEnv.isSelected())
                .setConnectionRetries(connectionRetries.getNumber())
                .setConnectionTimeout(connectionTimeout.getNumber())
                .build();

        return !serverConfig.equals(GlobalSettings.getInstance().getServerConfig());
    }

    @Override
    public void apply() throws ConfigurationException {
        ConfigVerificationUtils.validateGlobalConfig(serverConfig.getExcludedPaths(), serverConfig.getPolicyType(), serverConfig.getProject(), serverConfig.getWatches());
        GlobalSettings globalSettings = GlobalSettings.getInstance();
        globalSettings.updateConfig(serverConfig);
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE).update();
        connectionResults.setText("");
        loadConfig();
    }

    @Override
    public void reset() {
        loadConfig();
    }

    private Xray createXrayClient() {
        String urlStr = trim(xrayUrl.getText());
        return (Xray) new XrayClientBuilder()
                .setUrl(urlStr)
                .setUserName(trim(username.getText()))
                .setPassword(String.valueOf(password.getPassword()))
                .setAccessToken(String.valueOf(accessToken.getPassword()))
                .setUserAgent(USER_AGENT)
                .setInsecureTls(serverConfig.isInsecureTls())
                .setSslContext(serverConfig.getSslContext())
                .setProxyConfiguration(serverConfig.getProxyConfForTargetUrl(urlStr))
                .setLog(Logger.getInstance())
                .build();
    }

    private ArtifactoryManagerBuilder createArtifactoryManagerBuilder() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        String urlStr = trim(artifactoryUrl.getText());
        return new ArtifactoryManagerBuilder()
                .setServerUrl(urlStr)
                .setUsername(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword())
                .setAccessToken(serverConfig.getAccessToken())
                .setProxyConfiguration(serverConfig.getProxyConfForTargetUrl(urlStr))
                .setLog(Logger.getInstance())
                .setSslContext(serverConfig.isInsecureTls() ?
                        SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE).build() :
                        serverConfig.getSslContext());
    }

    private void loadConfig() {
        platformUrl.getEmptyText().setText("Example: https://acme.jfrog.io");
        xrayUrl.getEmptyText().setText("Example: https://acme.jfrog.io/xray");
        artifactoryUrl.getEmptyText().setText("Example: https://acme.jfrog.io/artifactory");
        excludedPaths.getEmptyText().setText("Example: " + DEFAULT_EXCLUSIONS);
        watches.getEmptyText().setText("Example: watch-1,watch-2");
        connectionResults.setText("");

        serverConfig = GlobalSettings.getInstance().getServerConfig();
        if (serverConfig != null) {
            updateConnectionDetailsTextFields();
            updatePolicyTextFields();
            excludedPaths.setText(serverConfig.getExcludedPaths());
            project.setText(serverConfig.getProject());
            watches.setText(serverConfig.getWatches());
            connectionRetries.setValue(serverConfig.getConnectionRetries());
            connectionTimeout.setValue(serverConfig.getConnectionTimeout());
            connectionDetailsFromEnv.setSelected(serverConfig.isConnectionDetailsFromEnv());
        } else {
            clearText(platformUrl, xrayUrl, artifactoryUrl, username, password);
            excludedPaths.setText(DEFAULT_EXCLUSIONS);
            allVulnerabilitiesRadioButton.setSelected(true);
            project.setText("");
            watches.setText("");
            connectionDetailsFromEnv.setSelected(false);
            connectionRetries.setValue(ConnectionRetriesSpinner.RANGE.initial);
            connectionTimeout.setValue(ConnectionTimeoutSpinner.RANGE.initial);
        }
    }

    private void updateConnectionDetailsTextFields() {
        platformUrl.setText(serverConfig.getUrl());
        xrayUrl.setText(serverConfig.getXrayUrl());
        artifactoryUrl.setText(serverConfig.getArtifactoryUrl());
        if (isNotBlank(serverConfig.getAccessToken())) {
            accessToken.setText(serverConfig.getAccessToken());
            accessTokenRadioButton.setSelected(true);
        } else {
            username.setText(serverConfig.getUsername());
            password.setText(serverConfig.getPassword());
        }
        if (!isAllBlank(xrayUrl.getText(), artifactoryUrl.getText()) && isBlank(platformUrl.getText())) {
            setRtAndXraySeparately.getModel().setSelected(true);
            setRtAndXraySeparately.getModel().setPressed(true);
            xrayUrl.setEnabled(true);
            artifactoryUrl.setEnabled(true);
        }
    }

    private void updatePolicyTextFields() {
        switch (ObjectUtils.defaultIfNull(serverConfig.getPolicyType(), ServerConfig.PolicyType.VULNERABILITIES)) {
            case WATCHES:
                accordingToWatchesRadioButton.setSelected(true);
                watches.setEnabled(true);
                watches.setText(serverConfig.getWatches());
                return;
            case PROJECT:
                accordingToProjectRadioButton.setSelected(true);
                watches.setEnabled(false);
                return;
            case VULNERABILITIES:
                allVulnerabilitiesRadioButton.setSelected(true);
                watches.setEnabled(false);
        }
    }
}
