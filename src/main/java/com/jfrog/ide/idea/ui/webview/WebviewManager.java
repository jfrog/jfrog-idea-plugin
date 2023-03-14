package com.jfrog.ide.idea.ui.webview;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
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
        handleLoadEvent(onLoadEnd);
        messagePacker = new MessagePacker(cefBrowser);
        return cefBrowser;
    }

    private void handleLoadEvent(Runnable onLoadEnd) {
        CefBrowser cefBrowser = jbCefBrowser.getCefBrowser();
        JBCefJSQuery openInBrowserQuery = JBCefJSQuery.create((JBCefBrowserBase) jbCefBrowser);
        Disposer.register(this, openInBrowserQuery);
        openInBrowserQuery.addHandler((link) -> {
            BrowserUtil.browse(link);
            return null;
        });

        String injectedCall = openInBrowserQuery.inject("link");
        jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                super.onLoadEnd(browser, frame, httpStatusCode);
                if (onLoadEnd != null) {
                    onLoadEnd.run();
                }

                // Add a click event listener to the whole page. Any click on an <a> element (even if it wasn't created
                // yet), will trigger this function which sends the link address to openInBrowserQuery.
                cefBrowser.executeJavaScript(
                        "function callback(e) {\n" +
                                "    if (e.target.tagName !== 'A')\n" +
                                "        return;\n" +
                                "    let link = e.target.href;\n" +
                                injectedCall +
                                "    e.preventDefault();\n" +
                                "}\n" +
                                "document.addEventListener('click', callback, false);",
                        cefBrowser.getURL(), 0);
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                super.onLoadError(browser, frame, errorCode, errorText, failedUrl);
                Logger.getInstance().error("An error occurred while opening the issue details view: " + errorText);
            }
        }, cefBrowser);
    }

    public void sendMessage(Object data) {
        messagePacker.send(data);
    }

    @Override
    public void dispose() {
    }
}
