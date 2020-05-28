package com.jfrog.ide.idea.navigation;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.Objects;

/**
 * Created by Bar Belity on 14/05/2020.
 */
public class NavigationTarget {

    private VirtualFile virtualFile;
    private int lineNumber;

    NavigationTarget(VirtualFile virtualFile, int lineNumber) {
        this.virtualFile = virtualFile;
        this.lineNumber = lineNumber;
    }

    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavigationTarget)) return false;
        NavigationTarget that = (NavigationTarget) o;
        return lineNumber == that.lineNumber &&
                Objects.equals(virtualFile, that.virtualFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(virtualFile, lineNumber);
    }
}
