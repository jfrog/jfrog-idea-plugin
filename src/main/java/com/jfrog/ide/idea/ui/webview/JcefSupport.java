package com.jfrog.ide.idea.ui.webview;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.ui.jcef.JBCefApp;
import com.jfrog.ide.idea.log.Logger;

/**
 * Whether the IDE can show the embedded browser.
 */
public final class JcefSupport {

    static final String JCEF_PLUGIN_ID = "com.intellij.modules.jcef";

    public enum Availability {
        AVAILABLE,
        PLUGIN_DISABLED,
        PLUGIN_MISSING,
        UNSUPPORTED_RUNTIME
    }

    private JcefSupport() {
    }

    /**
     * @return true when the embedded browser is on the classpath and reports itself supported.
     * A missing browser class is treated as unavailable, the same way a missing optional plugin is.
     */
    public static boolean isAvailable() {
        return availability() == Availability.AVAILABLE;
    }

    /**
     * Distinguishes a disabled Web Browser plugin, an IDE that does not ship it, and a runtime
     * that has the classes but does not support JCEF.
     */
    public static Availability availability() {
        PluginId pluginId = PluginId.getId(JCEF_PLUGIN_ID);
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
        if (plugin != null && PluginManagerCore.isDisabled(pluginId)) {
            return Availability.PLUGIN_DISABLED;
        }
        try {
            if (JBCefApp.isSupported()) {
                return Availability.AVAILABLE;
            }
            return Availability.UNSUPPORTED_RUNTIME;
        } catch (NoClassDefFoundError e) {
            Logger.getInstance().warn("JCEF is not available. The issue details view needs the Web Browser plugin.");
            return Availability.PLUGIN_MISSING;
        }
    }
}
