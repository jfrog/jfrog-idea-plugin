package com.jfrog.ide.idea.ui.webview;

import com.jfrog.ide.idea.log.Logger;
import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * We use this handler to read web resources that are saved inside the plugin's JAR.
 */
public class WebviewResourceHandler implements CefResourceHandler {
    private URL currUrl;
    private URLConnection connection;
    private InputStream webviewInputStream;
    private boolean error;

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String pathToResource = request.getURL().replace("http://jfrog-idea-plugin", "/jfrog-ide-webview");
        currUrl = WebviewResourceHandler.class.getResource(pathToResource);
        try {
            //noinspection DataFlowIssue
            connection = currUrl.openConnection();
            if (webviewInputStream != null) {
                webviewInputStream.close();
            }
            webviewInputStream = connection.getInputStream();
            error = false;
            callback.Continue();
            return true;
        } catch (IOException | NullPointerException e) {
            Logger.getInstance().error("An error occurred while reading webview resource: " + currUrl.toString(), e);
            error = true;
            return false;
        }
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
        if (error) {
            response.setError(CefLoadHandler.ErrorCode.ERR_FAILED);
            response.setStatus(500);
            return;
        }
        String url = currUrl.toString();
        String postfix = url.substring(url.lastIndexOf(".") + 1);
        switch (postfix) {
            case "css":
                response.setMimeType("text/css");
                break;
            case "js":
                response.setMimeType("text/javascript");
                break;
            case "html":
                response.setMimeType("text/html");
                break;
            default:
                response.setMimeType(connection.getContentType());
        }
        try {
            responseLength.set(webviewInputStream.available());
        } catch (IOException e) {
            Logger.getInstance().error("An error occurred while reading webview resource.", e);
            webviewInputStream = null;
            response.setError(CefLoadHandler.ErrorCode.ERR_FAILED);
            response.setStatus(500);
            return;
        }
        response.setStatus(200);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        try {
            int availableSize = webviewInputStream.available();
            if (availableSize < 1) {
                webviewInputStream.close();
                webviewInputStream = null;
                return false;
            }
            int maxBytesToRead = Math.min(availableSize, bytesToRead);
            bytesRead.set(webviewInputStream.read(dataOut, 0, maxBytesToRead));
        } catch (IOException e) {
            Logger.getInstance().error("An error occurred while reading webview resource.", e);
            webviewInputStream = null;
            return false;
        }
        return true;
    }

    @Override
    public void cancel() {
        try {
            if (webviewInputStream != null) {
                webviewInputStream.close();
            }
        } catch (IOException e) {
            // Do nothing
        }
        webviewInputStream = null;
    }
}
