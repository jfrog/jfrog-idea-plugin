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

    /**
     * Create a new Gradle project.
     *
     * @param baseDir  - Represents the base directory of the IntelliJ opened project
     * @param basePath - The directory where the build.gradle or the build.gradle.kts are located
     */
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
