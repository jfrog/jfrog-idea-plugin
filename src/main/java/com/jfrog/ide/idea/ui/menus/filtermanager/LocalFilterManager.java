package com.jfrog.ide.idea.ui.menus.filtermanager;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;

/**
 * @author yahavi
 */
@State(name = "LocalFilterState")
public class LocalFilterManager extends ConsistentFilterManager {

    public LocalFilterManager(Project project) {
        super(project);
    }

    public static LocalFilterManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, LocalFilterManager.class);
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_SCAN_FILTER_CHANGE;
    }
}
