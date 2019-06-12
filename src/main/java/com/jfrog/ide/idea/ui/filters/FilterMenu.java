package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.Events;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public abstract class FilterMenu<FilterType> extends JBPopupMenu {

    private SelectAllCheckbox<FilterType> selectAllCheckbox = new SelectAllCheckbox<>();
    private List<SelectionCheckbox> checkBoxMenuItems = Lists.newArrayList();
    Project mainProject;

    FilterMenu(@NotNull Project mainProject) {
        this.mainProject = mainProject;
    }

    /**
     * Add all menu's components in 3 steps: Clean, set listeners and add the required components.
     *
     * @param selectionMap map between FilterType and boolean that represents whether the filter is checked or not
     */
    void addComponents(@NotNull Map<FilterType, Boolean> selectionMap, boolean putUnknownLast, Topic<Events> event) {
        removeOldComponents();
        setListeners(selectionMap, event);
        addCheckboxes(putUnknownLast);
    }

    private void removeOldComponents() {
        checkBoxMenuItems.forEach(SelectionCheckbox::removeItemListeners);
        checkBoxMenuItems.clear();
        removeAll();
    }

    private void setListeners(Map<FilterType, Boolean> selectionMap, Topic<Events> event) {
        selectionMap.keySet().forEach(key -> checkBoxMenuItems.add(new SelectionCheckbox<>(selectionMap, key, event)));
        selectAllCheckbox.setListeners(selectionMap, checkBoxMenuItems, event);
    }

    private void addCheckboxes(boolean putUnknownLast) {
        add(selectAllCheckbox);
        if (putUnknownLast) {
            checkBoxMenuItems.stream()
                    .sorted(Comparator.comparing(item -> new License().getName().equals(item.getText()) ? 1 : -1))
                    .forEach(this::add);
        } else {
            checkBoxMenuItems.forEach(this::add);
        }
    }
}