package com.jfrog.ide.idea.ui.webview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.jcef.message.MessagePacker;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;

public class WebviewManager implements Disposable {
    private JBCefBrowser jbCefBrowser;
    private MessagePacker messagePacker;

    public CefBrowser createBrowser(String pageUri, Runnable onLoadEnd) {
        jbCefBrowser = new JBCefBrowser(pageUri);
        Disposer.register(this, jbCefBrowser);
        CefBrowser cefBrowser = jbCefBrowser.getCefBrowser();
        jbCefBrowser.createImmediately();
        jbCefBrowser.setOpenLinksInExternalBrowser(true);
        handleLoadEvent(onLoadEnd);
        messagePacker = new MessagePacker(cefBrowser);
        return cefBrowser;
    }

    private void handleLoadEvent(Runnable onLoadEnd) {
        jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                Logger.getInstance().info("Issue details view loaded successfully.");
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

    public void sendMessage(Object data) {
        messagePacker.send(data);
    }

    @Override
    public void dispose() {
    }
}
