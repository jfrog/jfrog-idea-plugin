package com.jfrog.ide.idea.ui.filters.filtermenu;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.ui.filters.filtermanager.CiFilterManager;
import com.jfrog.ide.idea.ci.CiManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Scope;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class CiScopeFilterMenu extends ScopeFilterMenu {

    public CiScopeFilterMenu(@NotNull Project mainProject) {
        super(mainProject);
    }

    @Override
    public void refresh() {
        // Get selected scopes
        Map<Scope, Boolean> selectedScopes = CiFilterManager.getInstance(mainProject).getSelectedScopes();

        // Hide the button if there are no scopes - for example in Go projects
        if (selectedScopes.size() == 1 && selectedScopes.containsKey(new Scope())) {
            filterButton.setVisible(false);
            return;
        }
        if (!filterButton.isVisible()) {
            filterButton.setVisible(true);
        }

        // Add checkboxes and triggers
        CiManager.getInstance(mainProject).getAllScopes()
                .stream()
                .filter(scope -> !selectedScopes.containsKey(scope))
                .forEach(scope -> selectedScopes.put(scope, true));
        addComponents(selectedScopes, true);
        super.refresh();
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_CI_FILTER_CHANGE;
    }
}