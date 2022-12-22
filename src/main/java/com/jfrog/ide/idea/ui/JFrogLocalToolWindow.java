package com.jfrog.ide.idea.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.tree.Artifact;
import com.jfrog.ide.common.tree.ImpactTreeNode;
import com.jfrog.ide.common.tree.Issue;
import com.jfrog.ide.common.tree.IssueOrLicense;
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.jcef.message.MessagePacker;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import com.jfrog.ide.idea.ui.webview.model.*;
import org.apache.commons.io.FileUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import com.jfrog.ide.idea.ui.webview.model.ExtendedInformation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.*;

/**
 * @author yahavi
 */
public class JFrogLocalToolWindow extends AbstractJFrogToolWindow {
    final LocalComponentsTree componentsTree;
    JBCefBrowserBase browser;
    JBCefJSQuery routerQuery;
    OnePixelSplitter verticalSplit;
    MessagePacker messagePacker;
    IssueOrLicense selectedIssue;

    /**
     * @param project   - Currently opened IntelliJ project
     */
    public JFrogLocalToolWindow(@NotNull Project project) {
        super(project);
        this.componentsTree = LocalComponentsTree.getInstance(project);
        browser = new JBCefBrowser();
        routerQuery = JBCefJSQuery.create(browser);
        routerQuery.addHandler((String message) -> {
            // TODO: change
            Logger.getInstance().info("You can call actions in the plugin from JCEF :)" + message);
            return null;
        });

        messagePacker = new MessagePacker(browser);
        initVulnerabilityInfoBrowser();

        JPanel toolbar = createActionToolbar();
        toolbar.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
        JPanel leftPanel = new JBPanel<>(new BorderLayout());
        leftPanel.add(toolbar, BorderLayout.PAGE_START);
        leftPanel.add(createComponentsTreeView());

        verticalSplit = new OnePixelSplitter(false, 0.4f);
        verticalSplit.setFirstComponent(leftPanel);
        // TODO: remove?
//        verticalSplit.setSecondComponent(browser.getComponent());

        setContent(verticalSplit);

        registerListeners();
    }

    @Override
    public JPanel createActionToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup(new CollapseAllAction(componentsTree), new ExpandAllAction(componentsTree));
        actionGroup.addAction(ActionManager.getInstance().getAction("JFrog.RefreshLocal"), Constraints.FIRST);
        return createJFrogToolbar(actionGroup);
    }

    /**
     * Register the issues tree listeners.
     */
    public void registerListeners() {
        // Xray credentials were set listener
        appBusConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, () ->
                ApplicationManager.getApplication().invokeLater(this::onConfigurationChange));

        // Component selection listener
        componentsTree.addTreeSelectionListener(e -> {
            // TODO: add tree selection logic here
            if (e == null || e.getNewLeadSelectionPath() == null || !(e.getNewLeadSelectionPath().getLastPathComponent() instanceof IssueOrLicense)) {
                // TODO: consider removing this and only allow closing the webview through the close button
                verticalSplit.setSecondComponent(null);
                return;
            }

            selectedIssue = (IssueOrLicense) e.getNewLeadSelectionPath().getLastPathComponent();
            updateIssueOrLicenseInWebview(selectedIssue);
            verticalSplit.setSecondComponent(browser.getComponent());
        });
        componentsTree.addRightClickListener();
        projectBusConnection.subscribe(ApplicationEvents.ON_SCAN_FILTER_CHANGE, () -> ApplicationManager.getApplication().invokeLater(() -> {
            LocalComponentsTree.getInstance(project).applyFiltersForAllProjects();
            // TODO: close the webview
        }));
        projectBusConnection.subscribe(ApplicationEvents.ON_SCAN_LOCAL_STARTED, () -> ApplicationManager.getApplication().invokeLater(this::resetViews));
    }

    private void initVulnerabilityInfoBrowser() {
        if (!JBCefApp.isSupported()) {
            // TODO: handle JCEF not supported
            Logger.getInstance().info("JCEF is not supported");
            return;
        }

        // TODO: consider moving to a separated method
        // TODO: not sure if it's the best way to create a temporary directory
        // TODO: this temp dir needs to be deleted at the end.
        Path tempDirPath;
        try {
            tempDirPath = Files.createTempDirectory("jfrog-idea-plugin");
            tempDirPath.toFile().deleteOnExit();
            FileUtils.copyToFile(getClass().getResourceAsStream("/jfrog-ide-webview-container/index.html"), new File(tempDirPath.toString(), "index.html"));
            FileUtils.copyToFile(getClass().getResourceAsStream("/jfrog-ide-webview-container/bundle.js"), new File(tempDirPath.toString(), "bundle.js"));

        } catch (IOException e) {
            // TODO: handle
            Logger.getInstance().error(e.getMessage());
            throw new RuntimeException(e);
        }

        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                injectJavaScriptCode(browser);
                updateIssueOrLicenseInWebview(selectedIssue);
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                // TODO: handle or remove this method
                Logger.getInstance().error("### onLoadError called");
            }
        }, browser.getCefBrowser());

        String pageUri = tempDirPath.resolve("index.html").toFile().toURI().toString();
        browser.loadURL(pageUri);
    }

    private void injectJavaScriptCode(CefBrowser browser) {
        String queryToInject = routerQuery.inject("json");
        browser.executeJavaScript(
                "window.api = {" +
                        "postMessage: function(message) {\n" +
                        "let json = JSON.stringify(message);\n" +
                        queryToInject +
                        "}" +
                        "};",
                browser.getURL(), 0);
    }

    /**
     * Create the components tree panel.
     *
     * @return the components tree panel
     */
    private JComponent createComponentsTreeView() {
        JPanel treePanel = new JBPanel<>(new GridLayout()).withBackground(UIUtil.getTableBackground());
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(componentsTree, ComponentUtils::getPathSearchString, true);
        treeSpeedSearch.getComponent().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treePanel.add(treeSpeedSearch.getComponent(), BorderLayout.WEST);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treePanel);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_BAR_SCROLLING_UNITS);
        treeScrollPane.setBorder(null);
        return treeScrollPane;
    }

    private void updateIssueOrLicenseInWebview(IssueOrLicense issueOrLicense) {
        if (issueOrLicense instanceof Issue) {
            Issue issue = (Issue) issueOrLicense;
            messagePacker.send("DEPENDENCY", convertIssueToDepPage(issue));
        } else {
            com.jfrog.ide.common.tree.License license = (com.jfrog.ide.common.tree.License) issueOrLicense;
            messagePacker.send("DEPENDENCY", convertLicenseToDepPage(license));
        }
    }

    private DependencyPage convertIssueToDepPage(Issue issue) {
        ExtendedInformation extendedInformation = null;
        if (issue.getResearchInfo() != null) {
            com.jfrog.ide.common.tree.ResearchInfo issueResearchInfo = issue.getResearchInfo();
            JfrogResearchSeverityReason[] severityReasons = Arrays.stream(issueResearchInfo.getSeverityReasons()).map(severityReason -> new JfrogResearchSeverityReason(severityReason.getName(), severityReason.getDescription(), severityReason.isPositive())).toArray(JfrogResearchSeverityReason[]::new);
            extendedInformation = new ExtendedInformation(issueResearchInfo.getShortDescription(), issueResearchInfo.getFullDescription(), issueResearchInfo.getSeverity().name(), issueResearchInfo.getRemediation(), severityReasons);
        }
        Artifact dependency = issue.getParentArtifact();
        return new DependencyPage(
                issue.getIssueId(),
                dependency.getGeneralInfo().getArtifactId(),
                dependency.getGeneralInfo().getPkgType(),
                dependency.getGeneralInfo().getVersion(),
                issue.getSeverity().name(),
                new License(dependency.getLicenseName(), "license-link"),
                issue.getSummary(),
                convertVersionRanges(issue.getFixedVersions()),
                convertVersionRanges(issue.getInfectedVersions()),
                new Reference[]{new Reference("www.reference.com", "reference-name")},
                new Cve(
                        issue.getCve().getCveId(),
                        issue.getCve().getCvssV2Score(),
                        issue.getCve().getCvssV2Vector(),
                        issue.getCve().getCvssV3Score(),
                        issue.getCve().getCvssV3Vector()
                ),
                convertImpactPath(dependency.getImpactPaths()),
                issue.getWatchNames().toArray(new String[0]),
                issue.getLastUpdated(),
                extendedInformation
        );
    }

    private DependencyPage convertLicenseToDepPage(com.jfrog.ide.common.tree.License license) {
        Artifact dependency = license.getParentArtifact();
        return new DependencyPage(
                license.getName(),
                dependency.getGeneralInfo().getArtifactId(),
                dependency.getGeneralInfo().getPkgType(),
                dependency.getGeneralInfo().getVersion(),
                license.getSeverity().name(),
                new License("", ""), // TODO: remove after
                null,
                null,
                new String[0],
                new Reference[]{new Reference("www.reference.com", "reference-name")},
                new Cve(null, null, null, null, null),
                convertImpactPath(dependency.getImpactPaths()),
                license.getWatchNames().toArray(new String[0]),
                license.getLastUpdated(),
                null
        );
    }

    private ImpactedPath convertImpactPath(ImpactTreeNode impactTreeNode) {
        ImpactedPath[] children = new ImpactedPath[impactTreeNode.getChildren().size()];
        for (int childIndex = 0; childIndex < children.length; childIndex++) {
            children[childIndex] = convertImpactPath(impactTreeNode.getChildren().get(childIndex));
        }
        return new ImpactedPath(removeComponentIdPrefix(impactTreeNode.getName()), children);
    }

    private String removeComponentIdPrefix(String compId) {
        final String prefixSeparator = "://";
        int prefixIndex = compId.indexOf(prefixSeparator);
        if (prefixIndex == -1) {
            return compId;
        }
        return compId.substring(prefixIndex + prefixSeparator.length());
    }

    // TODO: move this and the other conversion methods to another class
    private String[] convertVersionRanges(java.util.List<String> xrayVerRanges) {
        if (xrayVerRanges == null) {
            return new String[0];
        }
        return xrayVerRanges.stream().map(s -> convertVersionRange(s)).toArray(String[]::new);
    }

    private String convertVersionRange(String xrayVerRange) {
        final char upInclude = ']';
        final char upNotInclude = ')';
        final char downInclude = '[';
        final char downNotInclude = '(';

        final String lt = "<";
        final String lte = "≤";
        final String gt = ">";
        final String gte = "≥";
        final String versionPlacer = "version";
        final String allVersions = "All versions";

        boolean containsLeft = false;
        boolean containsRight = false;

        String[] parts = xrayVerRange.split(",");
        if (parts.length != 2) {
            if (parts.length == 1) {
                String singleVer = parts[0];
                if (singleVer.charAt(0) == downInclude && singleVer.charAt(singleVer.length() - 1) == upInclude) {
                    // Remove [ and ]
                    return singleVer.substring(1, singleVer.length() - 1);
                }
            }
            // Cannot convert
            return xrayVerRange;
        }

        String leftSide = parts[0];
        String rightSide = parts[1];
        if (leftSide.charAt(0) == downInclude) {
            containsLeft = true;
        } else if (leftSide.charAt(0) != downNotInclude) {
            // Cannot convert
            return xrayVerRange;
        }
        if (rightSide.charAt(rightSide.length() - 1) == upInclude) {
            containsRight = true;
        } else if (rightSide.charAt(rightSide.length() - 1) != upNotInclude) {
            // Cannot convert
            return xrayVerRange;
        }

        // Remove [
        String leftVer = leftSide.substring(1).trim();
        // Remove ]
        String rightVer = rightSide.substring(0, rightSide.length() - 1).trim();
        boolean leftEmpty = leftVer.isEmpty();
        boolean rightEmpty = rightVer.isEmpty();

        if (leftEmpty && rightEmpty) {
            return allVersions;
        }
        if (leftEmpty) {
            if (containsRight) {
                return lte + " " + rightVer;
            }
            return lt + " " + rightVer;
        }
        if (rightEmpty) {
            if (containsLeft) {
                return gte + " " + leftVer;
            }
            return gt + " " + leftVer;
        }

        // Left and right sides are not empty
        String res = leftVer + " ";
        if (containsLeft) {
            res += lte;
        } else {
            res += lt;
        }
        res += " " + versionPlacer + " ";
        if (containsRight) {
            res += lte;
        } else {
            res += lt;
        }
        res += " " + rightVer;
        return res;
    }

    /**
     * Clear the component tree.
     */
    void resetViews() {
        if (componentsTree != null) {
            componentsTree.reset();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        browser.dispose();
        routerQuery.dispose();
    }
}
