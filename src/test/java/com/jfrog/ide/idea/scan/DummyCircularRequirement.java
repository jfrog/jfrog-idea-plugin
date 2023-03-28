package com.jfrog.ide.idea.scan;

import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.requirement.PyRequirementVersionSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DummyCircularRequirement implements com.jetbrains.python.packaging.PyRequirement {
    private final String name;

    DummyCircularRequirement(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    @Override
    public @NotNull List<PyRequirementVersionSpec> getVersionSpecs() {
        return new ArrayList<>();
    }

    @Override
    public @NotNull List<String> getInstallOptions() {
        return new ArrayList<>();
    }

    @Override
    public @NotNull String getExtras() {
        return "";
    }

    @Override
    public @Nullable PyPackage match(@NotNull Collection<? extends PyPackage> collection) {
        return null;
    }
}
