package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Scope;

import java.util.Map;
import java.util.Set;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class ScopeFilterMenu extends FilterMenu<Scope> {

    public static final String NAME = "Scope";
    public static final String TOOLTIP = "Select scopes to show";

    public ScopeFilterMenu(@NotNull Project mainProject) {
        super(mainProject, NAME, TOOLTIP);
    }

    @Override
    public void refresh() {
        // Get selected scopes
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(mainProject);
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }
        Map<Scope, Boolean> selectedScopes = FilterManagerService.getInstance(mainProject).getSelectedScopes();

        // Hide the button if there are no scopes - for example in Go projects
        if (selectedScopes.size() == 1 && selectedScopes.containsKey(new Scope())) {
            filterButton.setVisible(false);
            return;
        }
        if (!filterButton.isVisible()) {
            filterButton.setVisible(true);
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
}