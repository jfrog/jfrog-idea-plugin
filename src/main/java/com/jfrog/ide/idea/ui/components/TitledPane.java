package com.jfrog.ide.idea.ui.components;

import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Yahav Itzhak on 13 Nov 2017.
 */
public class TitledPane extends JSplitPane {
    private final int location;

    /**
     * JSplitPane with a constant splitter location.
     * @param orientation JSplitPane.VERTICAL_SPLIT or JSplitPane.HORIZONTAL_SPLIT
     * @param location the location of the splitter
     * @param title the title
     * @param content the content
     */
    public TitledPane(int orientation, int location, Component title, Component content) {
        super(orientation);
        this.location = location;
        setTopComponent(title);
        setBottomComponent(content);
        setDividerLocation(location);
        setBackground(UIUtil.getTableBackground());
        setDividerSize(0);
    }

    @Override
    public int getDividerLocation() {
        return location ;
    }

    @Override
    public int getLastDividerLocation() {
        return location ;
    }
}