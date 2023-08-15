package com.jfrog.ide.idea.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider;
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.idea.events.AnnotationEvents;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.utils.Descriptor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yahavi
 **/
public class JFrogFloatingToolbar extends AbstractFloatingToolbarProvider implements Disposable {
    private final MessageBusConnection appBusConnection;
    private Set<String> changedFiles;
    private Set<FloatingToolbarComponent> components;


    public JFrogFloatingToolbar() {
        super("JFrog.Floating");
        changedFiles = new HashSet<>();
        components = new HashSet<>();
        this.appBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        registerOnChangeHandlers();
    }

    @Override
    public boolean getAutoHideable() {
        return false;
    }

    @Override
    public void register(@NotNull DataContext dataContext, @NotNull FloatingToolbarComponent component, @NotNull Disposable parentDisposable) {
        super.register(dataContext, component, parentDisposable);
        FileEditor fileEditor = dataContext.getData(PlatformDataKeys.FILE_EDITOR);
        if (fileEditor == null || fileEditor.getFile() == null) {
            return;
        }
        if (changedFiles.contains(fileEditor.getFile().getPath())) {
            component.scheduleShow();
            components.add(component);
            return;
        } else {
            component.scheduleHide();
        }
        Descriptor descriptor = Descriptor.fromFileName(fileEditor.getFile().getName());
        if (descriptor == null) {
            return;
        }

        Project project = dataContext.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        LocalComponentsTree localComponentsTree = LocalComponentsTree.getInstance(project);
        if (localComponentsTree.isCacheEmpty() || localComponentsTree.isCacheExpired()) {
            component.scheduleShow();
            components.add(component);
        }
    }

    private void registerOnChangeHandlers() {
        appBusConnection.subscribe(AnnotationEvents.ON_IRRELEVANT_RESULT, (AnnotationEvents) this::updateChangedFiles);
        appBusConnection.subscribe(ApplicationEvents.ON_SCAN_LOCAL_STARTED, (ApplicationEvents) this::clear);
    }

    private void clear() {
        this.changedFiles = new HashSet<>();
        components.forEach(FloatingToolbarComponent::scheduleHide);
        components = new HashSet<>();
    }

    private void updateChangedFiles(String s) {
        this.changedFiles.add(s);
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the application bus connection
        appBusConnection.disconnect();
    }
}
