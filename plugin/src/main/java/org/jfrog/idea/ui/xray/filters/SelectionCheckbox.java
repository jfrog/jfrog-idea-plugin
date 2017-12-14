package org.jfrog.idea.ui.xray.filters;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.Events;

import java.awt.event.ItemListener;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
class SelectionCheckbox<FilterType> extends MenuCheckbox {
    SelectionCheckbox(@NotNull Project project, @NotNull Map<FilterType, Boolean> selectionMap, @NotNull FilterType item) {
        setText(item.toString());
        setState(selectionMap.get(item));
        addItemListener(e -> {
            selectionMap.replace(item, isSelected());
            MessageBus messageBus = project.getMessageBus();
            messageBus.syncPublisher(Events.ON_SCAN_FILTER_CHANGE).update();
        });
    }

    public void removeItemListeners() {
        for (ItemListener itemListener : getItemListeners()) {
            removeItemListener(itemListener);
        }
    }
}