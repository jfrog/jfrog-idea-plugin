package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.Events;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemListener;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
class SelectionCheckbox<FilterType> extends MenuCheckbox {
    SelectionCheckbox(@NotNull Map<FilterType, Boolean> selectionMap, @NotNull FilterType item, Topic<Events> event) {
        setText(item.toString());
        setState(selectionMap.get(item));
        addItemListener(e -> {
            selectionMap.replace(item, isSelected());
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(event).update();
        });
    }

    void removeItemListeners() {
        for (ItemListener itemListener : getItemListeners()) {
            removeItemListener(itemListener);
        }
    }
}