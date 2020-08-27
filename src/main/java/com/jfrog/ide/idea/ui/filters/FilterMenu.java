package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.jfrog.ide.idea.ui.components.FilterButton;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public abstract class FilterMenu<FilterType> extends JBPopupMenu {

    private final SelectAllCheckbox<FilterType> selectAllCheckbox = new SelectAllCheckbox<>();
    private final List<SelectionCheckbox<FilterType>> checkBoxMenuItems = Lists.newArrayList();
    Project mainProject;
    FilterButton filterButton;

    FilterMenu(@NotNull Project mainProject, String name, String tooltip) {
        this.mainProject = mainProject;
        this.filterButton = new FilterButton(this, name, tooltip);
    }

    public void refresh() {
        filterButton.indicateFilterEnable(checkBoxMenuItems.stream().anyMatch(checkBoxMenuItem -> !checkBoxMenuItem.isSelected()));
    }

    public FilterButton getFilterButton() {
        return filterButton;
    }

    /**
     * Add all menu's components in 3 steps: Clean, set listeners and add the required components.
     *
     * @param selectionMap map between FilterType and boolean that represents whether the filter is checked or not
     */
    void addComponents(@NotNull Map<FilterType, Boolean> selectionMap, boolean putUnknownLast) {
        setListeners(selectionMap);
        addCheckboxes(putUnknownLast);
    }

    private void setListeners(Map<FilterType, Boolean> selectionMap) {
        selectionMap.keySet().stream()
                .filter(item -> checkBoxMenuItems.stream()
                        .map(AbstractButton::getText)
                        .noneMatch(text -> StringUtils.equals(text, item.toString())))
                .map(key -> new SelectionCheckbox<>(selectionMap, key))
                .forEach(checkBoxMenuItems::add);
        selectAllCheckbox.setListeners(selectionMap, checkBoxMenuItems);
    }

    private void addCheckboxes(boolean putUnknownLast) {
        if (isCheckboxNew(selectAllCheckbox)) {
            add(selectAllCheckbox);
        }
        if (putUnknownLast) {
            checkBoxMenuItems.stream()
                    .filter(this::isCheckboxNew)
                    .sorted(Comparator.comparing(item -> "Unknown".equals(item.getText())))
                    .forEach(this::add);
        } else {
            checkBoxMenuItems.forEach(this::add);
        }
    }

    /**
     * This method is a filter for adding the filter checkboxes.
     * If the filter is already exist in the list, we should not add it again.
     *
     * @param checkBoxMenuItem - The checkbox item to check.
     * @return true if the checkbox is new. False otherwise.
     */
    private boolean isCheckboxNew(MenuCheckbox checkBoxMenuItem) {
        return Arrays.stream(getComponents())
                .map(component -> (MenuCheckbox) component)
                .noneMatch(component -> component.getText().equals(checkBoxMenuItem.getText()));
    }
}