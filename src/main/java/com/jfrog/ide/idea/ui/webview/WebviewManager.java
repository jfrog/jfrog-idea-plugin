package com.jfrog.ide.idea.ui.webview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.webview.event.EventManager;
import com.jfrog.ide.idea.ui.webview.event.model.WebviewEvent;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

public class WebviewManager implements Disposable {
    private final JBCefBrowser jbCefBrowser;
    public EventManager eventManager;

    public WebviewManager(@NotNull Project project) {
        jbCefBrowser = new JBCefBrowser();
        // EventManager must be created before the webview is initialized
        eventManager = new EventManager(jbCefBrowser, project);
        Disposer.register(this, jbCefBrowser);
        jbCefBrowser.createImmediately();
        jbCefBrowser.setOpenLinksInExternalBrowser(true);
        streamConsoleMessagesToLog();
        handleLoadEvent(() -> eventManager.onWebviewLoadEnd());
        DumbService.getInstance(project).smartInvokeLater(() -> {
            // Register the scheme handler factory when in smart mode.
            // Performing this action immediately after opening IntelliJ sometimes results in a crash, particularly in IntelliJ 2022.3.
            CefApp.getInstance().registerSchemeHandlerFactory("http", "jfrog-idea-plugin", new WebviewSchemeHandlerFactory());
            jbCefBrowser.loadURL("http://jfrog-idea-plugin/index.html");
        });
    }

    public JBCefBrowser getBrowser() {
        return jbCefBrowser;
    }

    private void handleLoadEvent(Runnable onLoadEnd) {
        jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                Logger.getInstance().info("Issue details view loading ended with status code " + httpStatusCode);
                super.onLoadEnd(browser, frame, httpStatusCode);
                if (onLoadEnd != null) {
                    onLoadEnd.run();
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                super.onLoadError(browser, frame, errorCode, errorText, failedUrl);
                Logger.getInstance().error("An error occurred while opening the issue details view: " + errorText);
            }
        }, jbCefBrowser.getCefBrowser());
    }

    private void streamConsoleMessagesToLog() {
        jbCefBrowser.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                if (level == CefSettings.LogSeverity.LOGSEVERITY_VERBOSE) {
                    Logger.getInstance().debug(String.format("Webview console message: %s - %s", level, message));
                } else {
                    Logger.getInstance().info(String.format("Webview console message: %s - %s", level, message));
                }
                return false;
            }
        }, jbCefBrowser.getCefBrowser());
    }

    public void sendMessage(WebviewEvent.Type type, Object data) {
        eventManager.send(type, data);
    }

    @Override
    public void dispose() {
    }
}
