package com.jfrog.ide.idea.ui.utils;

import com.google.common.collect.Maps;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import java.util.Map;

/**
 * Created by romang on 4/12/17.
 */
public class IconUtils {
    private static final Icon defaultIcon = getIcon("unknown");
    private static final Map<String, Icon> icons = Maps.newHashMap();

    public static Icon load(String icon) {
        if (!icons.containsKey(icon)) {
            try {
                icons.put(icon, getIcon(icon));
            } catch (Exception e) {
                return defaultIcon;
            }
        }
        return icons.get(icon);
    }

    private static Icon getIcon(String icon) {
        return IconLoader.findIcon("/icons/" + icon.toLowerCase() + ".svg", IconUtils.class);
    }
}
