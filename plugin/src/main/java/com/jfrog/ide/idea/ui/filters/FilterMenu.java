package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class FilterMenu<FilterType> extends JBPopupMenu {
    protected Project project;
    private SelectAllCheckbox<FilterType> selectAllCheckbox = new SelectAllCheckbox<>();
    private List<SelectionCheckbox> checkBoxMenuItems = new CopyOnWriteArrayList<>();

    FilterMenu(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Add all menu's components in 3 steps: Clean, set listeners and add the required components.
     *
     * @param selectionMap map between FilterType and boolean that represents whether the filter is checked or not
     */
    void addComponents(@NotNull Map<FilterType, Boolean> selectionMap, boolean putUnknownLast) {
        removeOldComponents();
        setListeners(selectionMap);
        addCheckboxes(putUnknownLast);
    }

    private void removeOldComponents() {
        checkBoxMenuItems.forEach(SelectionCheckbox::removeItemListeners);
        checkBoxMenuItems.clear();
        removeAll();
    }

    private void setListeners(Map<FilterType, Boolean> selectionMap) {
        selectionMap.forEach((key, value) -> checkBoxMenuItems.add(new SelectionCheckbox<>(selectionMap, key)));
        selectAllCheckbox.setListeners(selectionMap, checkBoxMenuItems);
    }

    private void addCheckboxes(boolean putUnknownLast) {
        add(selectAllCheckbox);
        if (putUnknownLast) {
            checkBoxMenuItems
                    .stream()
                    .sorted(Comparator.comparing(item -> new License().getName().equals(item.getText()) ? 1 : -1))
                    .forEach(this::add);
        } else {
            checkBoxMenuItems.forEach(this::add);
        }
    }
}