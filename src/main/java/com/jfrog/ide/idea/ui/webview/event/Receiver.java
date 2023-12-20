package com.jfrog.ide.idea.ui.webview.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefClient;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.webview.event.model.IdeEvent;
import com.jfrog.ide.idea.ui.webview.event.tasks.JumpToCodeTask;
import com.jfrog.ide.idea.ui.webview.model.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.jfrog.ide.common.utils.Utils.createMapper;

/**
 * The Receiver class is responsible for handling events received from the webview in the IDE.
 * It sets up the necessary query handling and provides a mechanism to process the received events.
 */
public class Receiver {
    private final JBCefJSQuery query;
    JBCefBrowser jbBrowser;
    Project project;

    /**
     * @param jbBrowser The JBCefBrowser associated with the webview.
     * @param project   The Project associated with the IDE.
     */
    public Receiver(JBCefBrowser jbBrowser, @NotNull Project project) {
        this.project = project;
        this.jbBrowser = jbBrowser;
        jbBrowser.getJBCefClient().setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 5);
        // Queries must be created before the webview is initialized.
        query = JBCefJSQuery.create((JBCefBrowserBase) jbBrowser);
        query.addHandler((raw) -> {
            try {
                this.handler(unpack(raw));
            } catch (JsonProcessingException e) {
                Logger.getInstance().error(e.getMessage());
            }
            return null;
        });
    }

    /**
     * Unpacks the raw JSON string into an IdeEvent object.
     *
     * @param raw The raw JSON string to unpack.
     * @return The unpacked IdeEvent.
     * @throws JsonProcessingException If an error occurs during JSON processing.
     */
    public static IdeEvent unpack(String raw) throws JsonProcessingException {
        ObjectMapper ow = createMapper();
        return ow.readValue(raw, IdeEvent.class);
    }

    /**
     * Creates the body of the IDE send function with the specified function name.
     *
     * @param ideSendFunctionName The name of the IDE send function.
     * @return The body of the IDE send function as a string.
     */
    public String createIdeSendFuncBody(String ideSendFunctionName) {
        return "window['" + ideSendFunctionName + "'] = obj => { let raw = JSON.stringify(obj);  " + query.inject("raw") + ";}";
    }

    /**
     * Handles the received IdeEvent.
     *
     * @param event The received IdeEvent to handle.
     */
    private void handler(IdeEvent event) {
        if (Objects.requireNonNull(event.getType()) == IdeEvent.Type.JUMP_TO_CODE) {
            new JumpToCodeTask(this.project).execute(createMapper().convertValue(event.getData(), Location.class));
            Logger.getInstance().debug("Jump to " + event.getType());
        } else {
            Logger.getInstance().debug("Received unknown event from the webview: " + event.getType());
        }
    }
}