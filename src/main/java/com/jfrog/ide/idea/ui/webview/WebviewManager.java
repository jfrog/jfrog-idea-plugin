package com.jfrog.ide.idea.ui.webview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.JfrogContextMenuHandler;
import com.jfrog.ide.idea.ui.jcef.message.MessagePacker;
import com.jfrog.ide.idea.ui.jcef.message.MessageType;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;

public class WebviewManager implements Disposable {
    private JBCefBrowser jbCefBrowser;
    private MessagePacker messagePacker;
    private boolean schemeHandlerRegistered = false;

    public CefBrowser createBrowser(Runnable onLoadEnd) {
        jbCefBrowser = new JBCefBrowser();
        Disposer.register(this, jbCefBrowser);
        CefBrowser cefBrowser = jbCefBrowser.getCefBrowser();
        jbCefBrowser.createImmediately();
        jbCefBrowser.setOpenLinksInExternalBrowser(true);
        streamConsoleMessagesToLog();
        handleLoadEvent(onLoadEnd);
        messagePacker = new MessagePacker(cefBrowser);
        jbCefBrowser.getJBCefClient().addContextMenuHandler(new JfrogContextMenuHandler(), jbCefBrowser.getCefBrowser());
        return cefBrowser;
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

    public void sendMessage(MessageType type, Object data) {
        if (!schemeHandlerRegistered) {
            // Register the scheme handler factory right before the webview is first opened.
            // Performing this action immediately after opening IntelliJ sometimes results in a crash, particularly in IntelliJ 2022.3.
            CefApp.getInstance().registerSchemeHandlerFactory("http", "jfrog-idea-plugin", new WebviewSchemeHandlerFactory());
            jbCefBrowser.loadURL("http://jfrog-idea-plugin/index.html");
            schemeHandlerRegistered = true;
        }
        messagePacker.send(type, data);
    }

    @Override
    public void dispose() {
    }
}
