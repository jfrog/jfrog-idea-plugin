package com.jfrog.ide.idea.ui.menus.filtermanager;

import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author yahavi
 */
@State(name = "CiFilterState")
public class CiFilterManager extends ConsistentFilterManager {

    public CiFilterManager(Project project) {
        super(project);
    }

    public static CiFilterManager getInstance(@NotNull Project project) {
        return project.getService(CiFilterManager.class);
    }

    public void collectBuildsInformation(DependencyTree root) {
        clearBuilds();
        root.getChildren().stream()
                .map(DefaultMutableTreeNode::getUserObject)
                .map(Object::toString)
                .forEach(this::addBuild);
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_CI_FILTER_CHANGE;
    }
}
