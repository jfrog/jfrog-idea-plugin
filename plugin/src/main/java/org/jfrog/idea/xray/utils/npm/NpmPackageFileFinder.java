package org.jfrog.idea.xray.utils.npm;

import com.google.common.collect.Lists;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.jfrog.idea.xray.utils.Utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.jfrog.idea.xray.scan.NpmScanManager.INSTALLATION_DIR;

/**
 * Created by Yahav Itzhak on 17 Dec 2017.
 */
public class NpmPackageFileFinder implements FileVisitor<Path> {

    private static final List<String> EXCLUDED_DIRS = Lists.newArrayList("node_modules", INSTALLATION_DIR);
    private Path projectPath;
    private List<String> applicationPaths = Lists.newArrayList();
    static final Logger logger = Logger.getInstance(NpmPackageFileFinder.class);

    public NpmPackageFileFinder(Path projectPath) {
        this.projectPath = projectPath;
    }

    /**
     * Get package.json directories and their application names.
     * @return List of package.json's parent directories.
     */
    public List<String> getPackageFilePairs() throws IOException {
        Files.walkFileTree(projectPath, this);
        return applicationPaths;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return !isDirExcluded(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String fileName = file.getFileName().toString();
        if (isYarn(fileName)) {
            Utils.notify(logger, "JFrog Xray", "Yarn is not supported", NotificationType.INFORMATION, true);
            throw new IOException("Yarn is not supported");
        }
        if (isPackageFile(fileName)) {
            applicationPaths.add(file.getParent().toString());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        // Skip sub directories without permissions
        return exc == null ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
    }

    private static boolean isYarn(String fileName) {
        return "yarn.lock".equals(fileName);
    }

    private static boolean isPackageFile(String fileName) {
        return "package.json".equals(fileName);
    }

    private static boolean isDirExcluded(Path filePath) {
        for (String excludedDir : EXCLUDED_DIRS) {
            if (filePath.toString().contains(excludedDir)) {
                return true;
            }
        }
        return false;
    }
}
