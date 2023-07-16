package com.jfrog.ide.idea.ui.webview.event;

import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import com.jfrog.ide.idea.ui.webview.event.model.WebviewEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The EventManager responsible for managing events between the IDE and the Webview.
 * It handles the creation of a receiver and sender, allowing communication between the components.
 */
public class EventManager {
    public static String ideSendFuncName = "sendMessageToIdeFunc";
    Receiver receiver;
    Sender sender;

    /**
     * Constructs a new EventManager with the provided JBCefBrowser and Project.
     * Note: The eventManager must be created before the webview is initialized.
     *
     * @param jbBrowser The JBCefBrowser associated with the webview.
     * @param project   The Project associated with the IDE.
     */
    public EventManager(JBCefBrowser jbBrowser, @NotNull Project project) {
        this.receiver = new Receiver(jbBrowser, project);
        this.sender = new Sender(jbBrowser.getCefBrowser());
    }

    /**
     * Invoked when the webview finishes loading.
     * Creates the IDE send function body and sends it to the webview.
     */
    public void onWebviewLoadEnd() {
        String ideSendFuncBody = this.receiver.createIdeSendFuncBody(ideSendFuncName);
        this.sender.sendIdeSendFunc(ideSendFuncName, ideSendFuncBody);
    }

    /**
     * Sends an event of the specified type and data to the webview.
     *
     * @param type The type of the webview event.
     * @param data The data associated with the event.
     */
    public void send(WebviewEvent.Type type, Object data) {
        this.sender.sendEvent(type, data);
    }
}