package com.jfrog.ide.idea.ui.menus.builds;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.common.ci.BuildGeneralInfo;
import com.jfrog.ide.idea.Syncable;
import com.jfrog.ide.idea.ci.CiManager;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.events.BuildEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.menus.filtermanager.CiFilterManager;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class BuildsButton extends JComboBox<String> implements Syncable, Disposable {

    private ItemListener onSelectBuildListener;
    private final Project project;

    public BuildsButton(Project project) {
        this.project = project;
        setRenderer(new BuildsCellRenderer());
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_CI_FILTER_CHANGE;
    }

    public void setOnSelectBuildListener() {
        onSelectBuildListener = new OnSelectBuildListener();
        addItemListener(onSelectBuildListener);
    }

    public void removeOnSelectBuildListener() {
        if (onSelectBuildListener != null) {
            removeItemListener(onSelectBuildListener);
        }
    }

    private class OnSelectBuildListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedBuild = (String) e.getItem();
                CiFilterManager.getInstance(project).getSelectableBuilds()
                        .forEach(selectableItem -> selectableItem.setValue(StringUtils.equals(selectedBuild, selectableItem.getKey())));
                MessageBus messageBus = project.getMessageBus();
                messageBus.syncPublisher(getSyncEvent()).update();
                BuildGeneralInfo generalInfo = CiManager.getInstance(project).getBuildGeneralInfo(selectedBuild);
                if (generalInfo == null) {
                    String msg = String.format("Couldn't find build '%s' within the build results.", selectedBuild);
                    Logger.getInstance().error(msg);
                    return;
                }
                messageBus.syncPublisher(BuildEvents.ON_SELECTED_BUILD).update(generalInfo);
            }
        }
    }

    private static class BuildsCellRenderer extends DefaultListCellRenderer {
        @Override
        public void setIcon(Icon icon) {
            super.setIcon(AllIcons.General.GearPlain);
        }
    }

    @Override
    public void dispose() {
        removeOnSelectBuildListener();
    }
}
