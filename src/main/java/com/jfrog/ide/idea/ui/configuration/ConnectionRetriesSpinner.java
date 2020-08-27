package com.jfrog.ide.idea.ui.configuration;

import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;

/**
 * @author yahavi
 */
public class ConnectionRetriesSpinner extends JBIntSpinner {

    public static final UINumericRange RANGE = new UINumericRange(3, 0, 9);

    public ConnectionRetriesSpinner() {
        super(RANGE);
    }
}
