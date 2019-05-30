package com.jfrog.ide.idea.ui.filters;

import com.intellij.ui.components.JBCheckBoxMenuItem;

import java.awt.event.MouseEvent;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class MenuCheckbox extends JBCheckBoxMenuItem {
    @Override
    protected void processMouseEvent(MouseEvent evt) {
        if (evt.getID() == MouseEvent.MOUSE_RELEASED && contains(evt.getPoint())) {
            doClick();
            setArmed(true);
            return;
        }
        super.processMouseEvent(evt);
    }
}