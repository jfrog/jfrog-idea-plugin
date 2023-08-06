package com.jfrog.ide.idea.scan;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyRequirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DummyCircularDepPyPkgManager extends PyPackageManager {
    public static final String DIRECT_DEPENDENCY_NAME = "root";
    public static final String DIRECT_DEPENDENCY_VERSION = "1.0.0";
    public static final String CIRCULAR_DEPENDENCY_A = "a";
    public static final String CIRCULAR_DEPENDENCY_B = "b";
    public static final String CIRCULAR_DEPENDENCY_VERSION = "2.0.0";

    public DummyCircularDepPyPkgManager(@NotNull Sdk sdk) {
        super(sdk);
    }

    @Override
    public void installManagement() {

    }

    @Override
    public boolean hasManagement() {
        return false;
    }

    @Override
    public void install(@NotNull String s) {

    }

    @Override
    public void install(@Nullable List<PyRequirement> list, @NotNull List<String> list1) {

    }

    @Override
    public void uninstall(@NotNull List<PyPackage> list) {

    }

    @Override
    public void refresh() {

    }

    @Override
    public @NotNull String createVirtualEnv(@NotNull String s, boolean b) {
        return "";
    }

    @Override
    public @Nullable List<PyPackage> getPackages() {
        return null;
    }

    @Override
    public @NotNull List<PyPackage> refreshAndGetPackages(boolean bool) {
        // Create the following tree: root
        //                              |
        //                              a
        //                              |
        //                              b
        //                              |
        //                              a
        ArrayList<PyPackage> circularPyPackages = new ArrayList<>();
        // Root node
        ArrayList<PyRequirement> dependencies = new ArrayList<>();
        dependencies.add(new DummyCircularRequirement(CIRCULAR_DEPENDENCY_A));
        PyPackage root = new PyPackage(DIRECT_DEPENDENCY_NAME, DIRECT_DEPENDENCY_VERSION, null, dependencies);
        circularPyPackages.add(root);
        // a
        ArrayList<PyRequirement> aDependencies = new ArrayList<>();
        aDependencies.add(new DummyCircularRequirement(CIRCULAR_DEPENDENCY_B));
        PyPackage a = new PyPackage(CIRCULAR_DEPENDENCY_A, CIRCULAR_DEPENDENCY_VERSION, null, aDependencies);
        circularPyPackages.add(a);
        // b
        ArrayList<PyRequirement> bDependencies = new ArrayList<>();
        bDependencies.add(new DummyCircularRequirement(CIRCULAR_DEPENDENCY_A));
        PyPackage b = new PyPackage(CIRCULAR_DEPENDENCY_B, CIRCULAR_DEPENDENCY_VERSION, null, bDependencies);
        circularPyPackages.add(b);

        return circularPyPackages;
    }

    @Override
    public @Nullable List<PyRequirement> getRequirements(@NotNull Module module) {
        return null;
    }

    @Override
    public @Nullable PyRequirement parseRequirement(@NotNull String s) {
        return null;
    }

    @Override
    public @NotNull List<PyRequirement> parseRequirements(@NotNull String s) {
        return new ArrayList<>();
    }

    @Override
    public @NotNull List<PyRequirement> parseRequirements(@NotNull VirtualFile virtualFile) {
        return new ArrayList<>();
    }

    @Override
    public @NotNull Set<PyPackage> getDependents(@NotNull PyPackage pyPackage) {
        return new HashSet<>();
    }
}
