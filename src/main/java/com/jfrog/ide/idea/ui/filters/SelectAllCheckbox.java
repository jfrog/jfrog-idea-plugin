package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBCheckBoxMenuItem;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.Events;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
class SelectAllCheckbox<FilterType> extends MenuCheckbox {
    SelectAllCheckbox() {
        setText("All");
        setSelected(true);
    }

    void setListeners(@NotNull Map<FilterType, Boolean> selectionMap, @NotNull List<SelectionCheckbox> checkBoxMenuItems, Topic<Events> event) {
        removeListeners();
        addItemListener(e -> {
            selectionMap.entrySet().forEach(booleanEntry -> booleanEntry.setValue(isSelected()));

            for (JBCheckBoxMenuItem i : checkBoxMenuItems) {
                if (i.isSelected() != isSelected()) {
                    i.getModel().setPressed(isSelected());
                    i.getModel().setSelected(isSelected());
                }
            }
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(event).update();
        });
    }

    private void removeListeners() {
        for (ItemListener itemListener : getItemListeners()) {
            removeItemListener(itemListener);
        }
    }
}