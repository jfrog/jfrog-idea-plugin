package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBCheckBoxMenuItem;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class SelectAllCheckbox<FilterType> extends MenuCheckbox {

    private final Topic<ApplicationEvents> syncEvent;
    // If falsy, disable triggers
    private boolean active = true;

    public SelectAllCheckbox(Topic<ApplicationEvents> syncEvent) {
        this.syncEvent = syncEvent;
        setText("All");
        setSelected(true);
    }

    public void setListeners(@NotNull Map<FilterType, Boolean> selectionMap, @NotNull List<SelectionCheckbox<FilterType>> checkBoxMenuItems) {
        removeListeners();
        addItemListener(e -> {
            if (!active) {
                return;
            }
            selectionMap.entrySet().forEach(booleanEntry -> booleanEntry.setValue(isSelected()));

            for (JBCheckBoxMenuItem i : checkBoxMenuItems) {
                if (i.isSelected() != isSelected()) {
                    i.getModel().setPressed(isSelected());
                    i.getModel().setSelected(isSelected());
                }
            }
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(syncEvent).update();
        });
    }

    /**
     * Set button checked without triggering the listeners.
     *
     * @param checked - true if the button is checked, otherwise false
     */
    public void setChecked(boolean checked) {
        this.active = false;
        setSelected(checked);
        this.active = true;
    }

    private void removeListeners() {
        for (ItemListener itemListener : getItemListeners()) {
            removeItemListener(itemListener);
        }
    }
}