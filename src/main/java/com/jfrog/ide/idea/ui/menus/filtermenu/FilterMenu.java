package com.jfrog.ide.idea.ui.menus.filtermenu;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.Syncable;
import com.jfrog.ide.idea.ui.components.MenuButton;
import com.jfrog.ide.idea.ui.menus.MenuCheckbox;
import com.jfrog.ide.idea.ui.menus.SelectAllCheckbox;
import com.jfrog.ide.idea.ui.menus.SelectionCheckbox;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public abstract class FilterMenu<FilterType> extends ToolbarPopupMenu implements Syncable {

    private final SelectAllCheckbox<FilterType> selectAllCheckbox = new SelectAllCheckbox<>(getSyncEvent());
    private final List<SelectionCheckbox<FilterType>> checkBoxMenuItems = Lists.newArrayList();

    protected FilterMenu(@NotNull Project project, String name, String tooltip) {
        super(project, name, tooltip, AllIcons.General.Filter);
    }

    @Override
    public void refresh() {
        menuButton.indicateFilterEnable(checkBoxMenuItems.stream().anyMatch(checkBoxMenuItem -> !checkBoxMenuItem.isSelected()));
    }

    public MenuButton getFilterButton() {
        return menuButton;
    }

    /**
     * Add all menu's components in 3 steps: Set 'All' checkbox, set listeners and add the required components.
     *
     * @param selectionMap   - Map between FilterType and boolean that represents whether the filter is checked or not
     * @param putUnknownLast - Put the unknown checkbox last in the filters list
     */
    protected void addComponents(@NotNull Map<FilterType, Boolean> selectionMap, boolean putUnknownLast) {
        setSelectAllCheckbox(selectionMap);
        setListeners(selectionMap);
        addCheckboxes(putUnknownLast);
    }

    /**
     * Set 'All' checkbox. If all checkboxes are checked make 'All' checked.
     * Otherwise - If there is one filter applied make 'All' unchecked.
     *
     * @param selectionMap - map between FilterType and boolean that represents whether the filter is checked or not
     */
    private void setSelectAllCheckbox(Map<FilterType, Boolean> selectionMap) {
        selectAllCheckbox.setChecked(!selectionMap.containsValue(false));
    }

    private void setListeners(Map<FilterType, Boolean> selectionMap) {
        selectionMap.keySet().stream()
                .filter(item -> checkBoxMenuItems.stream()
                        .map(AbstractButton::getText)
                        .noneMatch(text -> StringUtils.equals(text, item.toString())))
                .map(key -> new SelectionCheckbox<>(selectionMap, key, getSyncEvent()))
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