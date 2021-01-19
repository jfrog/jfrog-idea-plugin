package com.jfrog.ide.idea.ui.components;


import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.jfrog.ide.idea.ui.configuration.Utils.setActiveForegroundColor;
import static com.jfrog.ide.idea.ui.configuration.Utils.setInactiveForegroundColor;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Represents success and failure icons near the Xray and Artifactory URL.
 *
 * @author yahavi
 **/
public class ConnectionResultsGesture extends MouseAdapter {
    private static final String SUCCESS_UNICODE = "\u2714";
    private static final String FAILURE_UNICODE = "\u2716";

    private final JLabel connectionResults;
    private String message;

    public ConnectionResultsGesture(JLabel connectionResults) {
        this.connectionResults = connectionResults;
    }

    /**
     * Set success icon.
     */
    public void setSuccess() {
        setValue("Success", SUCCESS_UNICODE);
    }

    /**
     * Set failure icon.
     *
     * @param message - The message to show in the hover and in the popup window
     */
    public void setFailure(String message) {
        setValue(message, FAILURE_UNICODE);
    }

    /**
     * Set success/failure icon and show the relevant message when the user click/hover on the relevant icon.
     *
     * @param message     - The message to show
     * @param unicodeIcon - Success/Failure icon
     */
    private void setValue(String message, String unicodeIcon) {
        if (isNotBlank(this.message)) {
            connectionResults.removeMouseListener(this);
        }
        this.message = message;
        connectionResults.setText(unicodeIcon);
        connectionResults.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        connectionResults.setToolTipText(message);
        connectionResults.addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JOptionPane.showMessageDialog(null, message);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setInactiveForegroundColor(connectionResults);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setActiveForegroundColor(connectionResults);
    }
}

