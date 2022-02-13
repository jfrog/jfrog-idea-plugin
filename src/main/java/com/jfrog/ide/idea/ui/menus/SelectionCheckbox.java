package com.jfrog.ide.idea.ui.menus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class SelectionCheckbox<FilterType> extends MenuCheckbox {
    public SelectionCheckbox(@NotNull Map<FilterType, Boolean> selectionMap, @NotNull FilterType item, Topic<ApplicationEvents> syncEvent) {
        setText(item.toString());
        setState(selectionMap.get(item));
        addItemListener(e -> {
            selectionMap.replace(item, isSelected());
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(syncEvent).update();
        });
    }
}