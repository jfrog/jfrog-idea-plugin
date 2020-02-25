package com.jfrog.ide.idea.projects;

import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yahavi
 */
public class NpmProject extends ProjectBase {
    public NpmProject(String basePath) {
        this.basePath = basePath;
    }

    public NpmProject(VirtualFile baseDir, String basePath) {
        this(basePath);
        Path packageJsonPath = Paths.get(baseDir.getPath()).relativize(Paths.get(basePath, "package.json"));
        this.virtualFile = baseDir.findFileByRelativePath(packageJsonPath.toString());
    }
}
