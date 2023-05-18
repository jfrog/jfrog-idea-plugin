package com.jfrog.ide.idea.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider;
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.utils.Descriptor;
import org.jetbrains.annotations.NotNull;

/**
 * @author yahavi
 **/
public class JFrogFloatingToolbar extends AbstractFloatingToolbarProvider {

    public JFrogFloatingToolbar() {
        super("JFrog.Floating");
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
        }
    }
}
