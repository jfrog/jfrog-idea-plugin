package com.jfrog.ide.idea.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.components.JBLabel;
import com.jfrog.ide.common.ci.BuildGeneralInfo;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.jfrog.ide.idea.ui.configuration.Utils.setActiveForegroundColor;
import static com.jfrog.ide.idea.ui.configuration.Utils.setInactiveForegroundColor;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yahavi
 **/
public class BuildLogButton extends JBLabel {
    private MouseListener mouseListener;

    public BuildLogButton() {
        setToolTipText("Click to view the build log");
        setInactiveForegroundColor(this);
    }

    public void initBuildLogButton(Project project, BuildGeneralInfo buildGeneralInfo) {
        removeMouseListener(mouseListener);
        String buildLog = buildGeneralInfo.getPath();
        if (isBlank(buildLog)) {
            setIcon(null);
            setText("");
            return;
        }
        setIcon(AllIcons.Ide.External_link_arrow);
        setText("Build Log");
        mouseListener = new BuildLogMouseAdapter(project, buildLog);
        addMouseListener(mouseListener);
    }

    private class BuildLogMouseAdapter extends MouseAdapter {
        private final String buildLog;
        private final Project project;

        private BuildLogMouseAdapter(Project project, String buildLog) {
            this.buildLog = buildLog;
            this.project = project;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            BrowserUtil.browse(buildLog);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setActiveForegroundColor(BuildLogButton.this);
            WindowManager.getInstance().getStatusBar(project).setInfo(buildLog);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setInactiveForegroundColor(BuildLogButton.this);
            WindowManager.getInstance().getStatusBar(project).setInfo(null);
        }
    }
}
