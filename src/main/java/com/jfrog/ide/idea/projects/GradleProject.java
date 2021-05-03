package com.jfrog.ide.idea.projects;

import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yahavi
 */
public class GradleProject extends ProjectBase {
    public GradleProject(String basePath) {
        this.basePath = basePath;
    }

    public GradleProject(VirtualFile baseDir, String basePath) {
        this(basePath);
        Path buildGradleFile = Paths.get(baseDir.getPath()).relativize(Paths.get(basePath, "build.gradle"));
        this.virtualFile = baseDir.findFileByRelativePath(buildGradleFile.toString());
        if (this.virtualFile == null) {
            buildGradleFile = Paths.get(baseDir.getPath()).relativize(Paths.get(basePath, "build.gradle.kts"));
            this.virtualFile = baseDir.findFileByRelativePath(buildGradleFile.toString());
        }
    }
}
