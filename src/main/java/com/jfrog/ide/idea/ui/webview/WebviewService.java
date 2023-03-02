package com.jfrog.ide.idea.ui.webview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.jfrog.ide.idea.log.Logger;
import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLoadHandlerAdapter;

import java.nio.file.Path;

import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;

public class WebviewService implements Disposable {
    private final Path JCEF_DIR = HOME_PATH.resolve("jcef");
    private CefBrowser browser;
    private Runnable onLoadEnd;

    public static WebviewService getInstance() {
        return ApplicationManager.getApplication().getService(WebviewService.class);
    }

    public CefBrowser initBrowser(String pageUri) throws Exception {
        if (browser != null) {
            browser.loadURL(pageUri);
            return browser;
        }

        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(JCEF_DIR.toFile());
        CefApp cefApp = builder.build();
        CefClient cefClient = cefApp.createClient();
        setLoadHandler(cefClient);

        CefMessageRouter msgRouter = CefMessageRouter.create();
        msgRouter.addHandler(new WebviewMessageRouter(), false);
        cefClient.addMessageRouter(msgRouter);

        // Non-deprecated variations of this method are not always available
        //noinspection deprecation
        browser = cefClient.createBrowser(pageUri, false, false);
        return browser;
    }

    private void setLoadHandler(CefClient cefClient) {
        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                super.onLoadEnd(browser, frame, httpStatusCode);
                onLoadEnd.run();

                // Add a click event listener to the whole page. Any click on an <a> element (even if it wasn't created
                // yet), will trigger this function which sends the link address to WebviewMessageRouter.
                browser.executeJavaScript(
                        "function callback(e) {\n" +
                                "    if (e.target.tagName !== 'A')\n" +
                                "        return;\n" +
                                "    var request_id = window.cefQuery({\n" +
                                "        request: e.target.href\n" +
                                "    });\n" +
                                "    e.preventDefault();\n" +
                                "}\n" +
                                "document.addEventListener('click', callback, false);\n",
                        browser.getURL(), 0);
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                super.onLoadError(browser, frame, errorCode, errorText, failedUrl);
                Logger.getInstance().error("An error occurred while opening the issue details view: " + errorText);
            }
        });
    }

    public void setOnLoadEnd(Runnable onLoadEnd) {
        this.onLoadEnd = onLoadEnd;
    }

    @Override
    public void dispose() {
        CefApp.getInstance().dispose();
    }
}
