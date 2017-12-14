package org.jfrog.idea.ui.xray.filters;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class FilterMenu<FilterType> extends JBPopupMenu {
    protected Project project;
    private SelectAllCheckbox<FilterType> selectAllCheckbox = new SelectAllCheckbox<>();
    private List<SelectionCheckbox> checkBoxMenuItems = Lists.newArrayList();

    FilterMenu(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Add all menu's components in 3 steps: Clean, set listeners and add the required components.
     * @param selectionMap map between FilterType and boolean that represents whether the filter is checked or not
     */
    void addComponents(@NotNull Map<FilterType, Boolean> selectionMap) {
        removeOldComponents();
        setListeners(selectionMap);
        addCheckboxes();
    }

    private void removeOldComponents() {
        checkBoxMenuItems.forEach(SelectionCheckbox::removeItemListeners);
        checkBoxMenuItems.clear();
        removeAll();
    }

    private void setListeners(Map<FilterType, Boolean> selectionMap) {
        selectionMap.forEach((key, value) -> checkBoxMenuItems.add(new SelectionCheckbox<>(project, selectionMap, key)));
        selectAllCheckbox.setListeners(project, selectionMap, checkBoxMenuItems);
    }

    private void addCheckboxes() {
        add(selectAllCheckbox);
        checkBoxMenuItems.forEach(this::add);
    }
}