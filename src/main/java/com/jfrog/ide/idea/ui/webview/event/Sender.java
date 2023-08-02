package com.jfrog.ide.idea.ui.webview.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.webview.event.model.WebviewEvent;
import org.cef.browser.CefBrowser;

import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.idea.ui.webview.event.model.WebviewEvent.Type.SET_EMITTER;

/**
 * The Sender class is responsible for sending events from the IDE to the webview.
 * It utilizes a CefBrowser instance to execute JavaScript code in the webview.
 */
public class Sender {
    CefBrowser browser;

    /**
     * @param browser The CefBrowser instance associated with the webview.
     */
    public Sender(CefBrowser browser) {
        this.browser = browser;
    }

    /**
     * Packs the webview event into a JSON string representation.
     *
     * @param type The type of the webview event.
     * @param data The data associated with the webview event.
     * @return The JSON string representation of the packed webview event.
     * @throws JsonProcessingException If an error occurs during JSON processing.
     */
    public static String pack(WebviewEvent.Type type, Object data) throws JsonProcessingException {
        ObjectMapper ow = createMapper();
        return ow.writeValueAsString(new WebviewEvent(type, data));
    }

    /**
     * Sends the IDE send function to the webview. This function allows sending data back from the webview to the IDE.
     *
     * @param ideSendFuncName The name of the IDE send function.
     * @param ideSendFuncBody The body of the IDE send function.
     */
    public void sendIdeSendFunc(String ideSendFuncName, String ideSendFuncBody) {
        // Send the function to jcef, this must be first before updating the webview.
        // Otherwise, the webview will not find any methods to use and will drop the request.
        this.send(ideSendFuncBody);
        // Update JFrog webview with the function to be used in order to send data back to the IDE.
        this.sendEvent(SET_EMITTER, "return " + ideSendFuncName);
    }

    /**
     * Sends a webview event with the specified type and data to the webview.
     *
     * @param type The type of the webview event.
     * @param data The data associated with the webview event.
     */
    public void sendEvent(WebviewEvent.Type type, Object data) {
        try {
            String raw = pack(type, data);
            Logger.getInstance().debug("Sending data to jfrog webview: " + raw);
            this.send("window.postMessage(" + raw + ")");
        } catch (JsonProcessingException e) {
            Logger.getInstance().error(e.getMessage());
        }
    }

    /**
     * Sends the specified event to the webview by executing the JavaScript code.
     *
     * @param event The JavaScript code to be executed in the webview.
     */
    public void send(String event) {
        browser.executeJavaScript(event, "", 0);
    }
}