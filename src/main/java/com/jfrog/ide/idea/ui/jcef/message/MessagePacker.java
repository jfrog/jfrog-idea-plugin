package com.jfrog.ide.idea.ui.jcef.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jfrog.ide.idea.log.Logger;
import org.cef.browser.CefBrowser;

public class MessagePacker implements MessagePipe {
    CefBrowser browser;

    public MessagePacker(CefBrowser browser) {
        this.browser = browser;
    }

    public void send(MessageType type, Object data) {
        try {
            String raw = MessagePipeSupport.Pack(type, data);
            Logger.getInstance().debug("Opening webview page with data: " + raw);
            this.send(raw);
        } catch (JsonProcessingException e) {
            Logger.getInstance().error(e.getMessage());
        }
    }

    private void send(String raw) {
        browser.executeJavaScript(
                "window.postMessage(" + raw + ")",
                browser.getURL(), 0);
    }
}
