package com.jfrog.ide.idea.ui.configuration;

import com.google.common.collect.Sets;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.*;
import com.intellij.util.Time;
import com.intellij.util.ui.AsyncProcessIcon;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.utils.XrayConnectionUtils;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.XrayClientBuilder;
import com.jfrog.xray.client.impl.util.JFrogInactiveEnvironmentException;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.access.AccessManager;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.client.response.CreateAccessTokenResponse;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.jfrog.ide.common.ci.Utils.createAqlForBuildArtifacts;
import static com.jfrog.ide.common.utils.Utils.*;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.isSupportedInXrayVersion;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.testComponentPermission;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.DEFAULT_EXCLUSIONS;
import static com.jfrog.ide.idea.ui.configuration.Utils.*;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * Created by romang on 1/29/17.
 */
public class JFrogGlobalConfiguration implements Configurable, Configurable.NoScroll {
    public static final String USER_AGENT = "jfrog-idea-plugin/" + JFrogGlobalConfiguration.class.getPackage().getImplementationVersion();
    private static final String SSO_LOGIN_FAILURE = """
            The SSO login option isn't available.
            Cause: %s

            Hints:
            1. Ensure that the JFrog Platform URL is correct
            2. The SSO login option is supported since Artifactory 7.64.0""";
    // All UI components
    private Set<JComponent> connectionDetailsEnabledComponents, connectionDetailsVisibleComponents;
    private Set<JComponent> webLoginEnabledComponents, webLoginVisibleComponents;
    private Set<JComponent> allUiComponents;
    private static final int SSO_WAIT_BETWEEN_RETRIES_MILLIS = 2 * Time.SECOND;
    private static final int SSO_RETRIES = 30;

    private ServerConfigImpl serverConfig;

    // Tabs
    private JPanel connectionDetails, settings, advanced;

    // Connection types
    private JRadioButton ssoLoginSelection, setCredentialsManuallySelection;

    // Connection details
    private JLabel platformUrlTitle;
    private JBTextField platformUrl;
    private JLabel usernameTitle;
    private JBTextField username;
    private JLabel passwordTitle;
    private JBPasswordField password;
    private JLabel accessTokenTitle;
    private JBPasswordField accessToken;
    private JButton loginButton;
    private JLabel authenticationMethodTitle;

    // Test connection
    private JButton testConnectionButton;
    private JBLabel connectionResults;
    private HyperlinkLabel infoPanel;

    // Advanced button
    private JXCollapsiblePane setSeparately;
    private JButton advancedExpandButton;
    private JLabel artifactoryUrlTitle;
    private JBTextField artifactoryUrl;
    private JLabel xrayUrlTitle;
    private JBTextField xrayUrl;


    // Authentication types
    private JRadioButton usernamePasswordRadioButton, accessTokenRadioButton;

    // Settings tab
    private JBTextField project;
    private HyperlinkLabel projectInstructions, policyInstructions, watchInstructions;
    private JRadioButton allVulnerabilitiesRadioButton, accordingToProjectRadioButton, accordingToWatchesRadioButton;
    private JBTextField watches;

    // Advanced tab
    private ConnectionRetriesSpinner connectionRetries;
    private ConnectionTimeoutSpinner connectionTimeout;
    private JBTextField excludedPaths;
    private ActionLink scanOptionsRestoreDefaultsActionLink;
    private ActionLink connectionOptionsRestoreDefaultsActionLink;
    private JRadioButton downloadResourcesFromReleasesRadioButton;
    private JRadioButton downloadResourcesThroughArtifactoryRadioButton;
    private JLabel repositoryNameJLabel;
    private JBTextField repositoryNameJBTextField;
    private JLabel repositoryNameDescJLabel;
    private JBLabel pluginResourcesDescJBLabel;
    private JBLabel releasesRepoLinkJBLabel;

    private int selectedTabIndex;


    public JFrogGlobalConfiguration() {
        createComponent();

        // Connection details
        initEnabledComponentSets();
        initAuthenticationMethod();
        initCredentialsTypeSelection();
        initLoginViaBrowserButton();
        initTestConnection();
        initAdvancedExpandButton();

        // Settings
        initPolicy();
        initLinks();

        // Advanced
        initConnectionOptionsRestoreDefaultsActionLink();
        initScanOptionsRestoreDefaultsActionLink();
        initPluginResourcesComponents();

        loadConfig();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.add("Connection Details", connectionDetails);
        tabbedPane.add("Settings", settings);
        tabbedPane.add("Advanced", advanced);
        tabbedPane.setSelectedIndex(selectedTabIndex);
        return tabbedPane;
    }

    public void selectSettingsTab() {
        selectedTabIndex = 1;
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
        return !createServerConfig().equals(GlobalSettings.getInstance().getServerConfig());
    }

    @Override
    public void apply() throws ConfigurationException {
        serverConfig = createServerConfig();
        ConfigVerificationUtils.validateGlobalConfig(serverConfig.getExcludedPaths(), serverConfig.getPolicyType(), serverConfig.getProject(), serverConfig.getWatches());
        GlobalSettings.getInstance().updateConfig(serverConfig);
    }

    @Override
    public void reset() {
        loadConfig();
    }

    /**
     * Initialize all UI components sets in order to show/hide them according to the user's choice.
     */
    private void initEnabledComponentSets() {
        allUiComponents = Sets.newHashSet(infoPanel, platformUrlTitle, platformUrl, xrayUrlTitle, xrayUrl,
                artifactoryUrlTitle, artifactoryUrl, username, password, accessTokenTitle, accessToken, accessTokenRadioButton, usernamePasswordRadioButton,
                loginButton, authenticationMethodTitle, usernameTitle, passwordTitle, advancedExpandButton, setSeparately, advancedExpandButton);

        webLoginEnabledComponents = webLoginVisibleComponents = Sets.newHashSet(infoPanel, platformUrlTitle, platformUrl, loginButton);

        connectionDetailsEnabledComponents = connectionDetailsVisibleComponents = Sets.newHashSet(infoPanel, platformUrlTitle, platformUrl,
                authenticationMethodTitle, usernamePasswordRadioButton, accessTokenRadioButton, usernameTitle, username,
                passwordTitle, password, accessTokenTitle, accessToken, advancedExpandButton, setSeparately, artifactoryUrlTitle,
                artifactoryUrl, xrayUrlTitle, xrayUrl);
    }

    /**
     * Create the server config according to the configured data in the UI.
     *
     * @return a new server config.
     */
    private ServerConfigImpl createServerConfig() {
        ServerConfigImpl.Builder builder = new ServerConfigImpl.Builder()
                .setConnectionType(getConnectionType())
                .setUrl(platformUrl.getText())
                .setArtifactoryUrl(artifactoryUrl.getText())
                .setXrayUrl(xrayUrl.getText())
                .setUsername(username.getText())
                .setPassword(String.valueOf(password.getPassword()))
                .setAccessToken(String.valueOf(accessToken.getPassword()))
                .setExcludedPaths(excludedPaths.getText())
                .setPolicyType(getPolicyType())
                .setProject(project.getText())
                .setWatches(watches.getText())
                .setConnectionRetries(connectionRetries.getNumber())
                .setConnectionTimeout(connectionTimeout.getNumber());
        if (downloadResourcesThroughArtifactoryRadioButton.isSelected()) {
            builder.setExternalResourcesRepo(repositoryNameJBTextField.getText());
        }
        return builder.build();
    }

    /**
     * Get the selected policy type - Project, Watches or all vulnerabilities.
     *
     * @return the selected policy type.
     */
    private ServerConfig.PolicyType getPolicyType() {
        if (accordingToProjectRadioButton.isSelected()) {
            return ServerConfig.PolicyType.PROJECT;
        } else if (accordingToWatchesRadioButton.isSelected()) {
            return ServerConfig.PolicyType.WATCHES;
        }
        return ServerConfig.PolicyType.VULNERABILITIES;
    }

    /**
     * Get the selected connection type - SSO or set the connection details manually.
     *
     * @return the selected connection type.
     */
    private ServerConfigImpl.ConnectionType getConnectionType() {
        return setCredentialsManuallySelection.isSelected() ?
                ServerConfigImpl.ConnectionType.CONNECTION_DETAILS :
                ServerConfigImpl.ConnectionType.SSO;
    }

    /**
     * Load the config and populate the UI fields.
     */
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
            if (!StringUtils.isEmpty(serverConfig.getExternalResourcesRepo())) {
                downloadResourcesThroughArtifactoryRadioButton.setSelected(true);
                repositoryNameJBTextField.setText(serverConfig.getExternalResourcesRepo());
            } else {
                downloadResourcesFromReleasesRadioButton.setSelected(true);
            }
        } else {
            clearText(platformUrl, xrayUrl, artifactoryUrl, username, password);
            excludedPaths.setText(DEFAULT_EXCLUSIONS);
            allVulnerabilitiesRadioButton.setSelected(true);
            project.setText("");
            watches.setText("");
            connectionRetries.setValue(ConnectionRetriesSpinner.RANGE.initial);
            connectionTimeout.setValue(ConnectionTimeoutSpinner.RANGE.initial);
            ssoLoginSelection.setSelected(true);
            downloadResourcesFromReleasesRadioButton.setSelected(true);
        }
        updateExternalRepositoryFields();
        initAuthMethodSelection();
    }

    /**
     * Initialize the test connection button.
     */
    private void initTestConnection() {
        testConnectionButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<String> results = new ArrayList<>();
            if (isBlank(platformUrl.getText()) && ssoLoginSelection.isSelected()) {
                results.add("JFrog platform URL is missing");
            } else {
                addIgnoreNull(results, checkXrayConnection());
                addIgnoreNull(results, checkArtifactoryConnection());
            }

            setConnectionResults(String.join("<br/>", results));
        }));
    }

    /**
     * Check connection to Xray.
     *
     * @return empty string for success, the reason if failed.
     */
    private String checkXrayConnection() {
        String url = resolveXrayUrl(xrayUrl.getText(), platformUrl.getText());
        if (isBlank(url)) {
            return "Xray URL is missing.";
        }
        try {
            Xray xrayClient = createXrayClient(url);

            setConnectionResults("Connecting to Xray...");
            connectionDetails.validate();
            connectionDetails.repaint();
            Version xrayVersion = xrayClient.system().version();

            // Check version
            if (!isSupportedInXrayVersion(xrayVersion)) {
                return XrayConnectionUtils.Results.unsupported(xrayVersion);
            }

            // Check permissions
            Pair<Boolean, String> testComponentPermissionRes = testComponentPermission(xrayClient);
            if (!testComponentPermissionRes.getLeft()) {
                throw new IOException(testComponentPermissionRes.getRight());
            }

            return XrayConnectionUtils.Results.success(xrayVersion);
        } catch (IOException exception) {
            if (exception instanceof JFrogInactiveEnvironmentException) {
                initHyperlinkLabel(infoPanel, "JFrog Platform is not active.\nClick <hyperlink>here</hyperlink> to activate it.", ((JFrogInactiveEnvironmentException) exception).getRedirectUrl());
            } else {
                createConnectionResultsBalloon(getLoginErrorMessage(exception), testConnectionButton);
            }

            return "Could not connect to JFrog Xray.";
        }
    }

    /**
     * Check connection to Artifactory.
     *
     * @return empty string for success, the reason if failed.
     */
    private String checkArtifactoryConnection() {
        String url = resolveArtifactoryUrl(artifactoryUrl.getText(), platformUrl.getText());
        if (isBlank(url)) {
            return "Artifactory URL is missing.";
        }
        try (ArtifactoryManager artifactoryManager = createArtifactoryManagerBuilder(url).build()) {
            setConnectionResults("Connecting to Artifactory...");
            connectionDetails.validate();
            connectionDetails.repaint();

            // Check connection.
            // This command will throw an exception if there is a connection or credentials issue.
            artifactoryManager.searchArtifactsByAql(createAqlForBuildArtifacts("*", "artifactory-build-info"));

            return "Successfully connected to Artifactory version: " + artifactoryManager.getVersion();
        } catch (Exception exception) {
            return "Could not connect to JFrog Artifactory.";
        }
    }

    /**
     * Get the login error after a failing connection testing.
     *
     * @param e - the received exception
     * @return the login error to display.
     */
    private String getLoginErrorMessage(Exception e) {
        String rootCause = substringBefore(ExceptionUtils.getRootCauseMessage(e), "<");
        if (setCredentialsManuallySelection.isSelected()) {
            return rootCause;
        }
        return String.format(SSO_LOGIN_FAILURE, rootCause);
    }

    /**
     * Create an Xray client from the configured details in the UI.
     *
     * @param xrayUrl - The Xray URL
     * @return an Xray client.
     */
    private Xray createXrayClient(String xrayUrl) {
        return (Xray) new XrayClientBuilder()
                .setUrl(xrayUrl)
                .setUserName(trim(username.getText()))
                .setPassword(String.valueOf(password.getPassword()))
                .setAccessToken(String.valueOf(accessToken.getPassword()))
                .setUserAgent(USER_AGENT)
                .setInsecureTls(serverConfig.isInsecureTls())
                .setSslContext(serverConfig.getSslContext())
                .setProxyConfiguration(serverConfig.getProxyConfForTargetUrl(xrayUrl))
                .setLog(Logger.getInstance())
                .build();
    }

    /**
     * Create an Artifactory client from the configured details in the UI.
     *
     * @param artifactoryUrl - The Artifactory URL
     * @return an Xray client.
     */
    private ArtifactoryManagerBuilder createArtifactoryManagerBuilder(String artifactoryUrl) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return new ArtifactoryManagerBuilder()
                .setServerUrl(artifactoryUrl)
                .setUsername(trim(username.getText()))
                .setPassword(String.valueOf(password.getPassword()))
                .setAccessToken(String.valueOf(accessToken.getPassword()))
                .setProxyConfiguration(serverConfig.getProxyConfForTargetUrl(artifactoryUrl))
                .setLog(Logger.getInstance())
                .setSslContext(createSSLContext(serverConfig));
    }

    /**
     * Set the connection results panel.
     *
     * @param results - The connection results to set.
     */
    private void setConnectionResults(String results) {
        if (results == null) {
            return;
        }
        connectionResults.setText("<html>" + results + "</html>");
    }


    /**
     * Init the "Advanced" button that displays the panel of Artifactory and Xray URLs.
     */
    private void initAdvancedExpandButton() {
        advancedExpandButton.setAction(setSeparately.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        advancedExpandButton.setText("Advanced");

        Action toggleAction = setSeparately.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION);
        toggleAction.putValue(JXCollapsiblePane.COLLAPSE_ICON, UIManager.getIcon("Tree.expandedIcon"));
        toggleAction.putValue(JXCollapsiblePane.EXPAND_ICON, UIManager.getIcon("Tree.collapsedIcon"));
    }

    /**
     * Init the "Login" button that do the SSO login.
     */
    private void initLoginViaBrowserButton() {
        loginButton.setIcon(AllIcons.Ide.External_link_arrow);
        loginButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (isBlank(platformUrl.getText())) {
                addRedBorder(platformUrl);
                return;
            }
            doSsoLogin();
        }));
    }

    /**
     * Do the SSO login.
     */
    private void doSsoLogin() {
        String uuid = UUID.randomUUID().toString();

        AsyncProcessIcon asyncProcessIcon = new AsyncProcessIcon("Connecting...");
        clearText(artifactoryUrl, xrayUrl, accessToken, username, password);
        loginButton.setText("");
        loginButton.setIcon(null);
        loginButton.add(asyncProcessIcon);
        loginButton.setEnabled(false);
        String urlStr = removeEnd(trim(platformUrl.getText()), "/") + "/access";
        try (AccessManager accessManager = new AccessManager(urlStr, "", Logger.getInstance())) {
            ProxyConfiguration proxyConfiguration = serverConfig.getProxyConfForTargetUrl(urlStr);
            if (proxyConfiguration != null) {
                accessManager.setProxyConfiguration(proxyConfiguration);
            }
            accessManager.setSslContext(serverConfig.isInsecureTls() ?
                    SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE).build() :
                    serverConfig.getSslContext());

            accessManager.sendBrowserLoginRequest(uuid);
            BrowserUtil.browse(removeEnd(platformUrl.getText(), "/") + "/ui/login?jfClientSession=" + uuid +
                    "&jfClientName=IDEA");

            for (int i = 0; i < SSO_RETRIES; i++) {
                CreateAccessTokenResponse response = accessManager.getBrowserLoginRequestToken(uuid);
                if (response != null) {
                    accessToken.setText(response.getAccessToken());
                    return;
                }
                Thread.sleep(SSO_WAIT_BETWEEN_RETRIES_MILLIS);
            }
        } catch (Exception e) {
            Logger.getInstance().error(ExceptionUtils.getRootCauseMessage(e), e);
        } finally {
            if (accessToken.getPassword().length == 0) {
                createConnectionResultsBalloon(String.format(SSO_LOGIN_FAILURE, "Timeout"), testConnectionButton);
            } else {
                testConnectionButton.doClick();
            }
            loginButton.remove(asyncProcessIcon);
            loginButton.setText("Login");
            loginButton.setIcon(AllIcons.Ide.External_link_arrow);
            loginButton.setEnabled(true);
        }
    }

    /**
     * Initialize the credentials type selection buttons.
     */
    private void initCredentialsTypeSelection() {
        setCredentialsManuallySelection.addItemListener(e -> {
            enableAndShowFields(e, connectionDetailsEnabledComponents, connectionDetailsVisibleComponents);
            initAuthMethodSelection();
        });
        ssoLoginSelection.addItemListener(e -> enableAndShowFields(e, webLoginEnabledComponents, webLoginVisibleComponents));
    }

    /**
     * Enable/Disable fields in the UI according to the selected values.
     *
     * @param event             - The button event
     * @param enabledComponents - The components to enable/disable, according to the event
     * @param visibleComponents - The components to show/hide, according to the event
     */
    private void enableAndShowFields(ItemEvent event, Set<JComponent> enabledComponents, Set<JComponent> visibleComponents) {
        JRadioButton cb = (JRadioButton) event.getSource();
        allUiComponents.forEach(component -> {
            component.setEnabled(cb.isSelected() && enabledComponents.contains(component));
            component.setVisible(cb.isSelected() && visibleComponents.contains(component));
        });
    }

    /**
     * Initialize the authentication method radio buttons.
     */
    private void initAuthenticationMethod() {
        accessTokenRadioButton.addItemListener(e -> {
            JRadioButton accessTokenButton = (JRadioButton) e.getSource();
            if (accessTokenButton.isSelected()) {
                accessToken.setEnabled(true);
                clearText(username, password);
                username.setEnabled(false);
                password.setEnabled(false);
                accessToken.setText(serverConfig.getAccessToken());
            } else {
                username.setEnabled(true);
                password.setEnabled(true);
                clearText(accessToken);
                accessToken.setEnabled(false);
                username.setText(serverConfig.getUsername());
                password.setText(serverConfig.getPassword());
            }
        });
    }

    /**
     * Initialize the auth method selection - access token or username and password.
     */
    private void initAuthMethodSelection() {
        boolean isAccessMode = isNotBlank(new String(accessToken.getPassword()));
        usernamePasswordRadioButton.setSelected(!isAccessMode);
        accessTokenRadioButton.setSelected(isAccessMode);
        accessToken.setEnabled(isAccessMode);
        username.setEnabled(!isAccessMode);
        password.setEnabled(!isAccessMode);
    }

    /**
     * Update the connection details from to the loaded server config.
     */
    void updateConnectionDetailsTextFields() {
        platformUrl.setText(serverConfig.getUrl());
        xrayUrl.setText(serverConfig.getXrayUrl());
        artifactoryUrl.setText(serverConfig.getArtifactoryUrl());
        if (isNotBlank(serverConfig.getAccessToken()) && setCredentialsManuallySelection.isSelected()) {
            accessToken.setText(serverConfig.getAccessToken());
            accessTokenRadioButton.setSelected(true);
        } else {
            username.setText(serverConfig.getUsername());
            password.setText(serverConfig.getPassword());
        }
        loadConnectionType();
    }

    /**
     * Load the connection type field from the loaded server config.
     */
    void loadConnectionType() {
        if (serverConfig.getConnectionType() == null) {
            // Keep legacy behavior
            setCredentialsManuallySelection.setSelected(true);
            return;
        }
        switch (serverConfig.getConnectionType()) {
            case SSO -> ssoLoginSelection.setSelected(true);
            case CONNECTION_DETAILS -> setCredentialsManuallySelection.setSelected(true);
        }
    }

    /**
     * Init the hyperlinks label in the "Settings" tab.
     */
    private void initLinks() {
        initHyperlinkLabel(projectInstructions, "Create a <hyperlink>JFrog Project</hyperlink>, or obtain the relevant JFrog Project key.", "https://www.jfrog.com/confluence/display/JFROG/Projects");
        initHyperlinkLabel(policyInstructions, "Create a <hyperlink>Policy</hyperlink> on JFrog Xray.", "https://www.jfrog.com/confluence/display/JFROG/Creating+Xray+Policies+and+Rules");
        initHyperlinkLabel(watchInstructions, "Create a <hyperlink>Watch</hyperlink> on JFrog Xray and assign your Policy and Project as resources to it.", "https://www.jfrog.com/confluence/display/JFROG/Configuring+Xray+Watches");
    }

    /**
     * Initialize the policy in the "Settings" tab.
     */
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

    /**
     * Update the policy text fields according to the selected policy type.
     */
    void updatePolicyTextFields() {
        switch (ObjectUtils.defaultIfNull(serverConfig.getPolicyType(), ServerConfig.PolicyType.VULNERABILITIES)) {
            case WATCHES -> {
                accordingToWatchesRadioButton.setSelected(true);
                watches.setEnabled(true);
                watches.setText(serverConfig.getWatches());
            }
            case PROJECT -> {
                accordingToProjectRadioButton.setSelected(true);
                watches.setEnabled(false);
            }
            case VULNERABILITIES -> {
                allVulnerabilitiesRadioButton.setSelected(true);
                watches.setEnabled(false);
            }
        }
    }

    /**
     * Initialize the "Restore Defaults" button of the "Connection Options" section in the "Advanced" tab.
     */
    private void initConnectionOptionsRestoreDefaultsActionLink() {
        connectionOptionsRestoreDefaultsActionLink.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            connectionRetries.setValue(ConnectionRetriesSpinner.RANGE.initial);
            connectionTimeout.setValue(ConnectionTimeoutSpinner.RANGE.initial);
        }));
    }

    /**
     * Initialize the "Restore Defaults" button of the "Scan Options" section in the "Advanced" tab.
     */
    private void initScanOptionsRestoreDefaultsActionLink() {
        scanOptionsRestoreDefaultsActionLink.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            excludedPaths.setText(DEFAULT_EXCLUSIONS);
        }));
    }

    /**
     * Initialize the "Plugin Resources" components in the "Advanced" tab.
     */
    private void initPluginResourcesComponents() {
        downloadResourcesFromReleasesRadioButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            updateExternalRepositoryFields();
        }));
        downloadResourcesThroughArtifactoryRadioButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            updateExternalRepositoryFields();
        }));

        // This is needed for the links in the labels to work
        pluginResourcesDescJBLabel.setCopyable(true);
        releasesRepoLinkJBLabel.setCopyable(true);
    }

    private void updateExternalRepositoryFields() {
        boolean enabled = downloadResourcesThroughArtifactoryRadioButton.isSelected();
        repositoryNameJLabel.setEnabled(enabled);
        repositoryNameJBTextField.setEnabled(enabled);
        repositoryNameDescJLabel.setEnabled(enabled);
    }
}
