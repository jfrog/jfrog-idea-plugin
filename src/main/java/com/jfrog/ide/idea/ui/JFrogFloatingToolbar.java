package com.jfrog.ide.idea.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider;
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.idea.events.AnnotationEvents;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.utils.Descriptor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yahavi
 **/
public class JFrogFloatingToolbar extends AbstractFloatingToolbarProvider implements Disposable {
    private MessageBusConnection projectBusConnection;
    private Map<String, FloatingToolbarComponent> filesToolbarComponents;
    private Set<String> changedFiles;


    public JFrogFloatingToolbar() {
        super("JFrog.Floating");
        filesToolbarComponents = new HashMap<>();
        changedFiles = new HashSet<>();
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
        Project project = dataContext.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        if (projectBusConnection == null) {
            projectBusConnection = project.getMessageBus().connect(this);
            registerOnChangeHandlers();
        }
        filesToolbarComponents.put(fileEditor.getFile().getPath(), component);
        if (changedFiles.contains(fileEditor.getFile().getPath())) {
            component.scheduleShow();
        }

        Descriptor descriptor = Descriptor.fromFileName(fileEditor.getFile().getName());
        if (descriptor == null) {
            return;
        }
        LocalComponentsTree localComponentsTree = LocalComponentsTree.getInstance(project);
        if (localComponentsTree.isCacheEmpty() || localComponentsTree.isCacheExpired()) {
            component.scheduleShow();
        }
    }

    private void registerOnChangeHandlers() {
        projectBusConnection.subscribe(AnnotationEvents.ON_IRRELEVANT_RESULT, (AnnotationEvents) this::updateFileChanged);
        projectBusConnection.subscribe(ApplicationEvents.ON_SCAN_LOCAL_STARTED, (ApplicationEvents) this::clear);
    }

    private void clear() {
        this.changedFiles = new HashSet<>();
        filesToolbarComponents.values().forEach(FloatingToolbarComponent::scheduleHide);
        filesToolbarComponents = new HashMap<>();
    }

    private void updateFileChanged(String s) {
        changedFiles.add(s);
        FloatingToolbarComponent jfrogToolBar = filesToolbarComponents.get(s);
        if (jfrogToolBar != null) {
            jfrogToolBar.scheduleShow();
        }
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the application bus connection
        projectBusConnection.disconnect();
    }
}
