package org.jfrog.idea.xray.scan;

import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.impl.MessageBusImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import java.nio.file.Paths;

public class NpmProjectImpl implements Project{

    private String bashPath = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "idea", "xray", "scan", "test")).toString();

    @NotNull
    @Override
    public String getName() {
        return null;
    }

    @Override
    public VirtualFile getBaseDir() {
        return null;
    }

    @Nullable
    @Override
    public String getBasePath() {
        return bashPath;
    }

    @Nullable
    @Override
    public VirtualFile getProjectFile() {
        return null;
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
        return null;
    }

    @Override
    public void save() {

    }

    @Override
    public boolean isOpen() {
        return false;
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
    public BaseComponent getComponent(@NotNull String name) {
        return null;
    }

    @Override
    public <T> T getComponent(@NotNull Class<T> interfaceClass) {
        return null;
    }

    @Override
    public <T> T getComponent(@NotNull Class<T> interfaceClass, T defaultImplementationIfAbsent) {
        return null;
    }

    @Override
    public boolean hasComponent(@NotNull Class interfaceClass) {
        return false;
    }

    @NotNull
    @Override
    public <T> T[] getComponents(@NotNull Class<T> baseClass) {
        return null;
    }

    @NotNull
    @Override
    public PicoContainer getPicoContainer() {
        return null;
    }

    @NotNull
    @Override
    public MessageBus getMessageBus() {
        return new MessageBusImpl.RootBus("");
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @NotNull
    @Override
    public <T> T[] getExtensions(@NotNull ExtensionPointName<T> extensionPointName) {
        return null;
    }

    @NotNull
    @Override
    public Condition getDisposed() {
        return null;
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
