package com.jfrog.ide.idea.ui.menus.filtermenu;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.ui.menus.filtermanager.LocalFilterManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Scope;

import java.util.Map;
import java.util.Set;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class LocalScopeFilterMenu extends ScopeFilterMenu {

    public LocalScopeFilterMenu(@NotNull Project project) {
        super(project);
    }

    @Override
    public void refresh() {
        // Get selected scopes
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(project);
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }
        Map<Scope, Boolean> selectedScopes = LocalFilterManager.getInstance(project).getSelectedScopes();

        // Hide the button if there are no scopes - for example in Go projects
        if (selectedScopes.size() == 1 && selectedScopes.containsKey(new Scope())) {
            menuButton.setVisible(false);
            return;
        }
        if (!menuButton.isVisible()) {
            menuButton.setVisible(true);
        }

        // Add checkboxes and triggers
        scanManagers.forEach(scanManager ->
                scanManager.getAllScopes()
                        .stream()
                        .filter(scope -> !selectedScopes.containsKey(scope))
                        .forEach(scope -> selectedScopes.put(scope, true)));
        addComponents(selectedScopes, true);
        super.refresh();
    }

    @Override
    public Topic<ApplicationEvents> getSyncEvent() {
        return ApplicationEvents.ON_SCAN_FILTER_CHANGE;
    }
}