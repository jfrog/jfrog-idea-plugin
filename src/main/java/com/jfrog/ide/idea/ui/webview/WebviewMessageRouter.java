package com.jfrog.ide.idea.ui.webview;

import com.intellij.ide.BrowserUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefNativeAdapter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;

public class WebviewMessageRouter extends CefNativeAdapter implements CefMessageRouterHandler {
    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        BrowserUtil.browse(request);
        return true;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
    }
}
