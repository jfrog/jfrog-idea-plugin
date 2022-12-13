package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.export.ExportMenu;
import com.jfrog.ide.idea.ui.jcef.message.MessagePacker;
import com.jfrog.ide.idea.ui.menus.filtermanager.LocalFilterManager;
import com.jfrog.ide.idea.ui.menus.filtermenu.*;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import com.jfrog.ide.idea.ui.webview.model.Cve;
import com.jfrog.ide.idea.ui.webview.model.DependencyPage;
import com.jfrog.ide.idea.ui.webview.model.ImpactedPath;
import com.jfrog.ide.idea.ui.webview.model.JfrogResearchSeverityReason;
import com.jfrog.ide.idea.ui.webview.model.License;
import com.jfrog.ide.idea.ui.webview.model.Reference;
import com.jfrog.ide.idea.ui.webview.model.ResearchInfo;
import org.apache.commons.io.FileUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.*;

/**
 * @author yahavi
 */
public class JFrogLocalToolWindow extends AbstractJFrogToolWindow {
    JBCefBrowserBase browser;
    JBCefJSQuery routerQuery;
    MessagePacker messagePacker;

    /**
     * @param project   - Currently opened IntelliJ project
     */
    public JFrogLocalToolWindow(@NotNull Project project) {
        super(project, LocalComponentsTree.getInstance(project));

        JPanel toolbar = createActionToolbar();
        browser = new JBCefBrowser();

        routerQuery = JBCefJSQuery.create(browser);
        routerQuery.addHandler((String message) -> {
            // TODO: change
            Logger.getInstance().info("You can call actions in the plugin from JCEF :)" + message);
            return null;
        });
        messagePacker = new MessagePacker(browser);
        initVulnerabilityInfoBrowser();

        OnePixelSplitter leftVerticalSplit = new OnePixelSplitter(false, 0.4f);
        leftVerticalSplit.setFirstComponent(createComponentsTreeView());
        leftVerticalSplit.setSecondComponent(browser.getComponent());

        setToolbar(toolbar);
        setContent(leftVerticalSplit);

        registerListeners();
    }

    @Override
    IssueFilterMenu createIssueFilterMenu() {
        return new LocalIssueFilterMenu(project);
    }

    @Override
    LicenseFilterMenu createLicenseFilterMenu() {
        return new LocalLicenseFilterMenu(project);
    }

    @Override
    ScopeFilterMenu createScopeFilterMenu() {
        return new LocalScopeFilterMenu(project);
    }

    @Override
    public JPanel createActionToolbar() {
        JPanel toolbarPanel = createComponentsTreePanel(true);
        // Add export menu
        ExportMenu exportMenu = new ExportMenu(project);
        componentsTree.addFilterMenu(exportMenu);
        toolbarPanel.add(exportMenu.getMenuButton());
        return toolbarPanel;
    }

    @Override
    public Set<Issue> getIssuesToDisplay(List<DependencyTree> selectedNodes) {
        return LocalFilterManager.getInstance(project).getFilteredScanIssues(selectedNodes);
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
            messagePacker.send("DEPENDENCY", new DependencyPage(
                    "dependency-id",
                    "dependency-name",
                    "type",
                    "version-1.2.3",
                    "Critical",
                    new License("license"),
                    "Summary-details",
                    new String[]{"1", "2"},
                    new String[]{"3", "4"},
                    new Reference[]{new Reference("reference-name", "www.reference.com")},
                    new Cve(
                            "cve-123",
                            "1",
                            "2",
                            "3",
                            "4"
                    ),
                    new ImpactedPath("path-name-1", new ImpactedPath[]{new ImpactedPath("sub-path-2", new ImpactedPath[]{})}),
                    new ResearchInfo(
                            "Insufficient validation in the json-schema package leads to prototype pollution",
                            "[JSON Schema](https://json-schema.org/) is a vocabulary that using for annotation and validation of JSON documents. It has 20M downloads per week for his [npm](https://www.npmjs.com/package/json-schema) repository.\r\n\r\n[Schema Validation](https://python-jsonschema.readthedocs.io/en/stable/validate/) is a process which validates an instance under a given schema, one of the way for validation is using the `validate()` method.\r\n\r\nAn attacker can cause a prototype pollution when sending a crafted schema to the `validate` or `checkPropertyChange` methods (1st or 2nd argument).\r\n\r\nThe prototype pollution vulnerability was discovered in the `checkObj()` function which does not validate the object type properly, allowing a remote attacker to pollute the object space using the `__proto__` attribute and cause the process to crash or cause a malicious object to pass validation. In case the attacker succeed to corrupt an object that supposed to be evaluated then he can execute arbitrary code on the affected system.\r\n\r\nA PoC can be seen in the [json-schema](https://github.com/kriszyp/json-schema/blob/master/test/tests.js) testing code:\r\n```js\r\nprototypePollution: function() {\r\n        console.log(\'testing\')\r\n        const instance = JSON.parse(`\r\n        {\r\n        \"$schema\":{\r\n            \"type\": \"object\",\r\n            \"properties\":{\r\n            \"__proto__\": {\r\n                \"type\": \"object\",\r\n                \r\n                \"properties\":{\r\n                \"polluted\": {\r\n                    \"type\": \"string\",\r\n                    \"default\": \"polluted\"\r\n                }\r\n                }\r\n            }\r\n            },\r\n            \"__proto__\": {}\r\n        }\r\n        }`);\r\n\r\n        const a = {};\r\n        validate(instance);\r\n        assert.equal(a.polluted, undefined);\r\n    }\r\n```",
                            "Low",
                            "##### Development mitigation\\n\\nAdd the `Object.freeze(Object.prototype);` directive once at the beginning of your main JS source code file (ex. `index.js`), preferably after all your `require` directives. This will prevent any changes to the prototype object, thus completely negating prototype pollution attacks.",
                            new JfrogResearchSeverityReason[]{new JfrogResearchSeverityReason(
                                    "The issue has an exploit published",
                                    "A public PoC demonstrated prototype pollution",
                                    "false"
                            ), new JfrogResearchSeverityReason(
                                    "The impact of exploiting the issue depends on the context of surrounding software. A severe impact such as RCE is not guaranteed.",
                                    "A prototype pollution attack allows the attacker to inject new properties to all JavaScript objects (but not set existing properties).\\r\\nTherefore, the impact of a prototype pollution attack depends on the way the JavaScript code uses any object properties after the attack is triggered.\\r\\nUsually, a DoS attack is possible since invalid properties quickly lead to an exception being thrown. In more severe cases, RCE may be achievable.",
                                    "true"
                            )}
                    )
            ));

            // TODO: add tree selection logic here
            if (e == null || e.getNewLeadSelectionPath() == null) {
                return;
            }
        });
        componentsTree.addOnProjectChangeListener(projectBusConnection);
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
        treePanel.add(treeSpeedSearch.getComponent(), BorderLayout.WEST);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treePanel);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_BAR_SCROLLING_UNITS);
        treeScrollPane.setBorder(null);
        return treeScrollPane;
    }

    @Override
    public void dispose() {
        super.dispose();
        browser.dispose();
        routerQuery.dispose();
    }
}
