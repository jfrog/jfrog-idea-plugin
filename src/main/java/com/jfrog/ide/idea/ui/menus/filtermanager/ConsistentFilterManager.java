package com.jfrog.ide.idea.ui.menus.filtermanager;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.idea.Syncable;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;
import org.jfrog.build.extractor.scan.Severity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yahavi
 */
public abstract class ConsistentFilterManager extends FilterManager implements PersistentStateComponent<ConsistentFilterManager.FiltersState>, Syncable {

    private final Project project;
    private FiltersState state;

    public ConsistentFilterManager(Project project) {
        this.project = project;
    }

    /**
     * Only on first scan after project opens, update the selected-licenses from state.
     * After updating, set state's licenses map to null.
     *
     * @return Selected licenses map according to persisted state.
     */
    public Map<License, Boolean> getSelectedLicenses() {
        Map<License, Boolean> selectedLicenses = super.getSelectedLicenses();
        if (state == null || state.selectedLicences == null) {
            return selectedLicenses;
        }

        // Previous state exists.
        for (License license : selectedLicenses.keySet()) {
            if (state.selectedLicences.containsKey(license.getName())) {
                selectedLicenses.put(license, state.selectedLicences.get(license.getName()));
            }
        }
        state.selectedLicences = null;

        // Update components tree with applied filters.
        if (selectedLicenses.containsValue(false)) {
            project.getMessageBus().syncPublisher(getSyncEvent()).update();
        }

        return selectedLicenses;
    }

    /**
     * Only on first scan after project opens, update the selected-scopes from state.
     * After updating, set state's scopes map to null.
     *
     * @return Selected scopes map according to persisted state.
     */
    @Override
    public Map<Scope, Boolean> getSelectedScopes() {
        Map<Scope, Boolean> selectedScopes = super.getSelectedScopes();
        if (state == null || state.selectedScopes == null) {
            return selectedScopes;
        }

        // Previous state exists.
        for (Scope scope : selectedScopes.keySet()) {
            if (state.selectedScopes.containsKey(scope.getName())) {
                selectedScopes.put(scope, state.selectedScopes.get(scope.getName()));
            }
        }
        state.selectedScopes = null;

        // Update components tree with applied filters.
        if (selectedScopes.containsValue(false)) {
            project.getMessageBus().syncPublisher(getSyncEvent()).update();
        }

        return selectedScopes;
    }

    static class FiltersState {
        public Map<Severity, Boolean> selectedSeverities;
        public Map<String, Boolean> selectedLicences;
        public Map<String, Boolean> selectedScopes;
    }

    @Override
    public FiltersState getState() {
        FiltersState state = new FiltersState();
        state.selectedSeverities = getSelectedSeverities();

        // Persist only license name and whether it is selected or not.
        state.selectedLicences = new HashMap<>();
        super.getSelectedLicenses().forEach((license, selected) -> state.selectedLicences.put(license.getName(), selected));

        // Persist only scope name and whether it is selected or not.
        state.selectedScopes = new HashMap<>();
        super.getSelectedScopes().forEach((scope, selected) -> state.selectedScopes.put(scope.getName(), selected));

        return state;
    }

    @Override
    public void loadState(@NotNull FiltersState state) {
        this.state = state;
        setSelectedSeverities(state.selectedSeverities);
    }
}
