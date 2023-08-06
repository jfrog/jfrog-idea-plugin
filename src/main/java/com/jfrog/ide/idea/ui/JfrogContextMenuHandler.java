package com.jfrog.ide.idea.ui;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandler;

import javax.swing.*;
import java.awt.*;

public class JfrogContextMenuHandler implements CefContextMenuHandler {
    private static final int DEV_TOOLS_ID = 1;

    @Override
    public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
        model.clear();
        model.addItem(DEV_TOOLS_ID, "Inspect");
    }

    @Override
    public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame, CefContextMenuParams params, int commandId, int eventFlags) {
        if (commandId == DEV_TOOLS_ID) {
            openDevTools(browser);
            return true;
        }
        return false;
    }

    private void openDevTools(CefBrowser browser) {
        SwingUtilities.invokeLater(() -> {
            CefBrowser devToolsBrowser = browser.getDevTools();
            JFrame frame = new JFrame("DevTools - JFrog IDE Webview");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setSize(800, 600);
            frame.getContentPane().add(devToolsBrowser.getUIComponent(), BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }

    @Override
    public void onContextMenuDismissed(CefBrowser browser, CefFrame frame) {
    }
}