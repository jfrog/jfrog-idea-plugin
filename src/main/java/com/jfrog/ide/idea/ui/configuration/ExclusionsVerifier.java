package com.jfrog.ide.idea.ui.configuration;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.nio.file.FileSystems;
import java.util.regex.PatternSyntaxException;

/**
 * Input verifier for the "Excluded paths" field in the UI configuration.
 * This verifier make sure the input glob pattern is legal.
 *
 * @author yahavi
 **/
public class ExclusionsVerifier extends InputVerifier {
    public static final String EXCLUSIONS_PREFIX = "**/*";
    public static final String EXCLUSIONS_SUFFIX = "*";

    public static String EXCLUSIONS_REGEX_PARSER = ".*\\{(.*)\\}\\*";

    public static final String DEFAULT_EXCLUSIONS = EXCLUSIONS_PREFIX + "{.idea,test, node_modules}" + EXCLUSIONS_SUFFIX;

    private final JTextField excludedPaths;

    public ExclusionsVerifier(JTextField excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    @Override
    public boolean shouldYieldFocus(JComponent input, JComponent target) {
        if (verify(input)) {
            return true;
        }
        excludedPaths.setText(DEFAULT_EXCLUSIONS);
        return false;
    }

    @Override
    public boolean verify(JComponent input) {
        if (StringUtils.isBlank(excludedPaths.getText()) || !StringUtils.startsWith(excludedPaths.getText(), EXCLUSIONS_PREFIX)
                || !StringUtils.endsWith(excludedPaths.getText(), EXCLUSIONS_SUFFIX)) {
            return false;
        }
        try {
            FileSystems.getDefault().getPathMatcher("glob:" + excludedPaths.getText());
        } catch (PatternSyntaxException e) {
            return false;
        }
        return true;
    }
}
