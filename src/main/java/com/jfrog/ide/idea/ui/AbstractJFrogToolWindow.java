package com.jfrog.ide.idea.ui;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
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
    final ComponentsTree componentsTree;
    final Project project;

    // TODO: fix comment
    /**
     * @param project   - Currently opened IntelliJ project
     * @param componentsTree - .
     */
    public AbstractJFrogToolWindow(@NotNull Project project, ComponentsTree componentsTree) {
        super(true);
        this.projectBusConnection = project.getMessageBus().connect(this);
        this.appBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        this.componentsTree = componentsTree;
        this.project = project;
    }

    /**
     * Create the action toolbar. That is the top toolbar.
     *
     * @return the action toolbar
     */
    abstract JPanel createActionToolbar();

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

    /**
     * Clear the component tree.
     */
    void resetViews() {
        if (componentsTree != null) {
            componentsTree.reset();
        }
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the project bus connection
        projectBusConnection.disconnect();
        // Disconnect and release resources from the application bus connection
        appBusConnection.disconnect();
    }
}
