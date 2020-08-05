package com.jfrog.ide.idea.ui.configuration;

import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;

/**
 * @author yahavi
 */
public class ConnectionTimeoutSpinner extends JBIntSpinner {

    public static final UINumericRange RANGE = new UINumericRange(60, 10, 3600);

    public ConnectionTimeoutSpinner() {
        super(RANGE);
    }
}
