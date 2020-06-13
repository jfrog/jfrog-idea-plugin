package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Severity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yahavi
 */
@State(name = "FilterState")
public class FilterManagerService extends FilterManager implements PersistentStateComponent<FilterManagerService.FiltersState> {

    private FiltersState state;

    public static FilterManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, FilterManagerService.class);
    }

    /**
     * Only on first scan after project opens, update the selected-licenses from state.
     * After updating, set state's licenses map to null.
     * @return Selected licenses map according to persisted state.
     */
    @Override
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

        // Update licenses tree with applied filters.
        if (selectedLicenses.containsValue(false)) {
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(ApplicationEvents.ON_SCAN_FILTER_LICENSES_CHANGE).update();
        }

        return selectedLicenses;
    }

    static class FiltersState {
        public Map<Severity, Boolean> selectedSeverities;
        public Map<String, Boolean> selectedLicences;
    }

    @Override
    public FiltersState getState() {
        FiltersState state = new FiltersState();
        state.selectedSeverities = getSelectedSeverities();

        // Persist only license name and whether it is selected or not.
        Map<License, Boolean> selectedLicences = super.getSelectedLicenses();
        state.selectedLicences = new HashMap<>();
        for (License license : selectedLicences.keySet()) {
            state.selectedLicences.put(license.getName(), selectedLicences.get(license));
        }

        return state;
    }

    @Override
    public void loadState(@NotNull FiltersState state) {
        this.state = state;
        setSelectedSeverities(state.selectedSeverities);
    }
}
