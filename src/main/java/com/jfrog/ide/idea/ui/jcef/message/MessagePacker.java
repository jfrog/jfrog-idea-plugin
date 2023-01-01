package com.jfrog.ide.idea.ui.jcef.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.jfrog.ide.idea.log.Logger;

import static com.jfrog.ide.idea.ui.jcef.message.PackedMessage.IDE_SEND_FUNCTION_NAME;

public class MessagePacker implements MessagePipe {
    JBCefBrowserBase browser;

    public MessagePacker(JBCefBrowserBase browser) {
        this.browser = browser;
    }

    public void send(String type, Object data) {
        try {
            String raw = MessagePipeSupport.Pack(type, data);
            this.send(raw);
        } catch (JsonProcessingException e) {
            Logger.getInstance().error(e.getMessage());
        }
    }

    private void send(String raw) {
        browser.getCefBrowser().executeJavaScript(
                "window." + IDE_SEND_FUNCTION_NAME + "=" + raw,
                browser.getCefBrowser().getURL(), 0);
        browser.getCefBrowser().executeJavaScript(
                "window.postMessage(" + raw + ")",
                browser.getCefBrowser().getURL(), 0);
    }
}
