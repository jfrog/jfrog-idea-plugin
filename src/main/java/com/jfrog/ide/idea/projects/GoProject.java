package com.jfrog.ide.idea.projects;

import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Bar Belity on 12/02/2020.
 */
public class GoProject extends ProjectBase {
    public GoProject(String basePath) {
        this.basePath = basePath;
    }

    public GoProject(VirtualFile baseDir, String basePath) {
        this(basePath);
        Path goModPath = Paths.get(baseDir.getPath()).relativize(Paths.get(basePath, "go.mod"));
        this.virtualFile = baseDir.findFileByRelativePath(goModPath.toString());
    }
}
