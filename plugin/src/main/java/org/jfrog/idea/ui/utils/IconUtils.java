package org.jfrog.idea.ui.utils;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Created by romang on 4/12/17.
 */
public class IconUtils {

    private static Icon defaultIcon = IconLoader.findIcon("/icons/default.png");

    public static Icon load(String severity) {
        try {
            return IconLoader.findIcon("/icons/" + severity.toLowerCase() + ".png");
        } catch (Exception e) {
            return defaultIcon;
        }
    }
}
