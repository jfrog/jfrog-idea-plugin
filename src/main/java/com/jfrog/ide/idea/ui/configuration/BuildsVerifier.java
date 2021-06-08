package com.jfrog.ide.idea.ui.configuration;

import javax.swing.*;
import java.nio.file.FileSystems;
import java.util.regex.PatternSyntaxException;

/**
 * Input verifier for the "Builds pattern" field in the UI configuration.
 * This verifier make sure the input glob pattern is legal.
 *
 * @author yahavi
 **/
public class BuildsVerifier extends InputVerifier {
    private final JTextField buildsPattern;

    public BuildsVerifier(JTextField buildsPattern) {
        this.buildsPattern = buildsPattern;
    }

    // For JDK < 9
    @SuppressWarnings("deprecation")
    public boolean shouldYieldFocus(JComponent input) {
        return shouldYieldFocus(input, null);
    }

    // For JDK >= 9
    public boolean shouldYieldFocus(JComponent input, JComponent ignore) {
        if (verify(input)) {
            return true;
        }
        buildsPattern.setText("");
        return false;
    }

    @Override
    public boolean verify(JComponent input) {
        try {
            FileSystems.getDefault().getPathMatcher("glob:" + buildsPattern.getText());
        } catch (PatternSyntaxException e) {
            return false;
        }
        return true;
    }
}
