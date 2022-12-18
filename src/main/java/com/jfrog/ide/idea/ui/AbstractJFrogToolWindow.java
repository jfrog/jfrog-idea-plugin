package com.jfrog.ide.idea.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author yahavi
 */
public abstract class AbstractJFrogToolWindow extends SimpleToolWindowPanel implements Disposable {
    final MessageBusConnection projectBusConnection;
    final MessageBusConnection appBusConnection;
    final Project project;

    /**
     * @param project   - Currently opened IntelliJ project
     */
    public AbstractJFrogToolWindow(@NotNull Project project) {
        super(true);
        this.projectBusConnection = project.getMessageBus().connect(this);
        this.appBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        this.project = project;
    }

    /**
     * Create the action toolbar. That is the top toolbar.
     *
     * @return the action toolbar
     */
    abstract JPanel createActionToolbar();

    /**
     * Clear the component tree.
     */
    abstract void resetViews();

    JPanel createJFrogToolbar(ActionGroup actionGroup) {
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("JFrog toolbar", actionGroup, true);
        actionToolbar.setTargetComponent(this);
        JPanel toolbarPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarPanel.add(actionToolbar.getComponent());
        return toolbarPanel;
    }

    /**
     * Called after a change in the credentials.
     */
    public void onConfigurationChange() {
        resetViews();
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the project bus connection
        projectBusConnection.disconnect();
        // Disconnect and release resources from the application bus connection
        appBusConnection.disconnect();
    }
}
