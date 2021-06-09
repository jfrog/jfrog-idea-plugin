package com.jfrog.ide.idea.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.components.JBLabel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.jfrog.ide.idea.ui.configuration.Utils.setActiveForegroundColor;
import static com.jfrog.ide.idea.ui.configuration.Utils.setInactiveForegroundColor;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yahavi
 **/
public class LinkButton extends JBLabel {
    private MouseListener mouseListener;

    public LinkButton(String tooltip) {
        setToolTipText(tooltip);
        setInactiveForegroundColor(this);
    }

    public void init(Project project, String text, String link) {
        removeMouseListener(mouseListener);
        if (isBlank(link)) {
            setIcon(null);
            setText("");
            return;
        }
        setIcon(AllIcons.Ide.External_link_arrow);
        setText(text);
        mouseListener = new BuildLogMouseAdapter(project, link);
        addMouseListener(mouseListener);
    }

    private class BuildLogMouseAdapter extends MouseAdapter {
        private final String link;
        private final Project project;

        private BuildLogMouseAdapter(Project project, String link) {
            this.link = link;
            this.project = project;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            BrowserUtil.browse(link);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setActiveForegroundColor(LinkButton.this);
            WindowManager.getInstance().getStatusBar(project).setInfo(link);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setInactiveForegroundColor(LinkButton.this);
            WindowManager.getInstance().getStatusBar(project).setInfo(null);
        }
    }
}
