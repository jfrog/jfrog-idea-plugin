package com.jfrog.ide.idea.projects;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.pico.DefaultPicoContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

/**
 * Unlike maven or gradle, for npm and go, there there's no real project in IntelliJ. We therefore use this project.
 * Created by Bar Belity on 19/02/2020.
 */
public class ProjectBase implements Project {
    protected String basePath;
    protected VirtualFile virtualFile;

    @NotNull
    @Override
    public String getName() {
        return basePath;
    }

    @Override
    public VirtualFile getBaseDir() {
        return null;
    }

    @Nullable
    @Override
    public String getBasePath() {
        return basePath;
    }

    @Nullable
    @Override
    public VirtualFile getProjectFile() {
        return virtualFile;
    }

    @Nullable
    @Override
    public String getProjectFilePath() {
        return null;
    }

    @Nullable
    @Override
    public String getPresentableUrl() {
        return null;
    }

    @Nullable
    @Override
    public VirtualFile getWorkspaceFile() {
        return null;
    }

    @NotNull
    @Override
    public String getLocationHash() {
        return getName();
    }

    @Override
    public void save() {

    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public <T> T getComponent(@NotNull Class<T> interfaceClass) {
        return null;
    }

    @Override
    public boolean hasComponent(@NotNull Class interfaceClass) {
        return false;
    }

    @NotNull
    @Override
    public PicoContainer getPicoContainer() {
        return new DefaultPicoContainer();
    }

    @NotNull
    @Override
    public MessageBus getMessageBus() {
        return ApplicationManager.getApplication().getMessageBus();
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @NotNull
    @Override
    public Condition<?> getDisposed() {
        return ApplicationManager.getApplication().getDisposed();
    }

    @Override
    public void dispose() {

    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

    }
}
