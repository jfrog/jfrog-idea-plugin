package com.jfrog.ide.idea.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
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
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.apache.commons.io.FileUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.*;

/**
 * @author yahavi
 */
public class JFrogLocalToolWindow extends AbstractJFrogToolWindow {
    JBCefBrowserBase browser;
    JBCefJSQuery routerQuery;

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

        initVulnerabilityInfoBrowser();

        OnePixelSplitter leftVerticalSplit = new OnePixelSplitter(false, 0.4f);
        leftVerticalSplit.setFirstComponent(createComponentsTreeView());
        leftVerticalSplit.setSecondComponent(browser.getComponent());

        setToolbar(toolbar);
        setContent(leftVerticalSplit);

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
            FileUtils.copyToFile(getClass().getResourceAsStream("/webview/index.html"), new File(tempDirPath.toString(), "index.html"));
            FileUtils.copyToFile(getClass().getResourceAsStream("/webview/index.js"), new File(tempDirPath.toString(), "index.js"));
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
