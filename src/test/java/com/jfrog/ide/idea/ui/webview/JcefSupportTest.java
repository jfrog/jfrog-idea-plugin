package com.jfrog.ide.idea.ui.webview;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.jcef.JBCefApp;
import com.jfrog.ide.idea.ui.JFrogLocalToolWindow;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JcefSupportTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String JCEF_PLUGIN_ID = "com.intellij.modules.jcef";

    public void testUnsupportedBrowserShowsMissingComponentMessage() {
        try (MockedStatic<JBCefApp> jcef = Mockito.mockStatic(JBCefApp.class)) {
            jcef.when(JBCefApp::isSupported).thenReturn(false);
            String text = openWindowText();
            assertTrue(text.contains("JCEF that seem to be missing"));
            assertTrue(text.contains("different boot runtime"));
        }
    }

    public void testDisabledBrowserPluginAsksToEnableIt() {
        IdeaPluginDescriptor jcefPlugin = Mockito.mock(IdeaPluginDescriptor.class);
        try (MockedStatic<PluginManagerCore> plugins = Mockito.mockStatic(PluginManagerCore.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<JBCefApp> jcef = Mockito.mockStatic(JBCefApp.class)) {
            plugins.when(() -> PluginManagerCore.getPlugin(Mockito.any())).thenAnswer(invocation ->
                    jcefPluginId(invocation.getArgument(0)) ? jcefPlugin : invocation.callRealMethod());
            plugins.when(() -> PluginManagerCore.isDisabled(Mockito.any())).thenAnswer(invocation ->
                    jcefPluginId(invocation.getArgument(0)) || (boolean) invocation.callRealMethod());
            jcef.when(JBCefApp::isSupported).thenReturn(false);

            String text = openWindowText();
            assertTrue(text.contains("Enable Web Browser (JCEF) in Settings › Plugins"));
            assertFalse(text.contains("different boot runtime"));
        }
    }

    public void testMissingBrowserPluginSaysDetailsAreUnavailable() {
        try (MockedStatic<PluginManagerCore> plugins = Mockito.mockStatic(PluginManagerCore.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<JBCefApp> jcef = Mockito.mockStatic(JBCefApp.class)) {
            plugins.when(() -> PluginManagerCore.getPlugin(Mockito.any())).thenAnswer(invocation ->
                    jcefPluginId(invocation.getArgument(0)) ? null : invocation.callRealMethod());
            jcef.when(JBCefApp::isSupported).thenThrow(new NoClassDefFoundError("com/intellij/ui/jcef/JBCefApp"));

            String text = openWindowText();
            assertTrue(text.contains("Vulnerability details aren't available in this IDE"));
            assertFalse(text.contains("different boot runtime"));
        }
    }

    private String openWindowText() {
        JFrogLocalToolWindow window = new JFrogLocalToolWindow(getProject());
        try {
            assertNotNull("results tree should be built when the browser is missing",
                    LocalComponentsTree.getInstance(getProject()).getParent());
            return componentText(window.getContent());
        } finally {
            window.dispose();
        }
    }

    private static boolean jcefPluginId(PluginId pluginId) {
        return JCEF_PLUGIN_ID.equals(pluginId.getIdString());
    }

    private static String componentText(Component component) {
        StringBuilder text = new StringBuilder();
        appendText(component, text);
        return text.toString();
    }

    private static void appendText(Component component, StringBuilder text) {
        if (component instanceof JLabel label && label.getText() != null) {
            text.append(label.getText());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                appendText(child, text);
            }
        }
    }

    public void testBrowserDependencyIsOptional() throws Exception {
        try (InputStream in = JcefSupportTest.class.getClassLoader().getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull(in);
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xml.contains("<depends config-file=\"with-jcef.xml\" optional=\"true\">com.intellij.modules.jcef</depends>"));
        }
    }
}
