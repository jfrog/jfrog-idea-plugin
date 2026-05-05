package com.jfrog.ide.idea.utils;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jfrog.build.extractor.WslUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

/**
 * Compares filesystem paths that may refer to the same WSL-backed file but use different spellings
 * (e.g. {@code \\wsl$\Distro\...} from {@link java.nio.file.Path} vs {@code //wsl$/Distro/...} from IntelliJ VFS).
 * <p>
 * WSL UNC normalization and Linux-path conversion delegate to {@link WslUtils} (build-info-extractor).
 * IntelliJ-style {@code //wsl$/...} URLs are converted to Windows UNC here because that form is IDE-specific.
 */
public final class DescriptorPathUtils {

    private DescriptorPathUtils() {
    }

    /**
     * Resolves a local filesystem path to a {@link VirtualFile}, trying alternate spellings relevant for WSL mounts.
     */
    @Nullable
    public static VirtualFile findLocalVirtualFile(@NotNull String filePath) {
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        VirtualFile vf = lfs.findFileByPath(filePath);
        if (vf != null) {
            return vf;
        }
        vf = lfs.findFileByIoFile(new File(filePath));
        if (vf != null) {
            return vf;
        }
        String unc = intellijWslUrlToUnc(filePath);
        if (!unc.equals(filePath)) {
            vf = lfs.findFileByPath(unc);
            if (vf != null) {
                return vf;
            }
            vf = lfs.findFileByIoFile(new File(unc));
            if (vf != null) {
                return vf;
            }
        }
        if (SystemUtils.IS_OS_WINDOWS && filePath.indexOf('\\') >= 0) {
            vf = lfs.findFileByPath(filePath.replace('\\', '/'));
            if (vf != null) {
                return vf;
            }
        }
        return null;
    }

    /**
     * Returns true if the two paths refer to the same descriptor file or directory, including WSL path variants.
     */
    public static boolean areDescriptorPathsEqual(String pathA, String pathB) {
        if (pathA == null || pathB == null) {
            return pathA == pathB;
        }
        if (pathA.equals(pathB)) {
            return true;
        }
        if (trySameFile(pathA, pathB)) {
            return true;
        }
        return Objects.equals(toCompareKey(pathA), toCompareKey(pathB));
    }

    private static boolean trySameFile(String pathA, String pathB) {
        Path a = tryParsePath(pathA);
        Path b = tryParsePath(pathB);
        if (a == null || b == null) {
            return false;
        }
        try {
            if (Files.exists(a) && Files.exists(b)) {
                return Files.isSameFile(a, b);
            }
        } catch (IOException | SecurityException ignored) {
            // fall through to string compare
        }
        return false;
    }

    private static Path tryParsePath(String path) {
        try {
            return Paths.get(path);
        } catch (InvalidPathException e) {
            String converted = intellijWslUrlToUnc(path);
            if (converted.equals(path)) {
                return null;
            }
            try {
                return Paths.get(converted);
            } catch (InvalidPathException e2) {
                return null;
            }
        }
    }

    /**
     * Converts IntelliJ-style WSL URLs ({@code //wsl$/Distro/...}) to Windows UNC for {@link WslUtils}.
     */
    public static String intellijWslUrlToUnc(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String p = path;
        if (startsWithIgnoreCase(p, "//wsl$/")) {
            return "\\\\wsl$\\" + p.substring("//wsl$/".length()).replace('/', '\\');
        }
        if (startsWithIgnoreCase(p, "//wsl.localhost/")) {
            return "\\\\wsl.localhost\\" + p.substring("//wsl.localhost/".length()).replace('/', '\\');
        }
        return path;
    }

    /**
     * Converts a SARIF {@code artifactLocation.uri} to a path the IDE can open locally.
     * <p>
     * On Windows, {@link Paths#get(URI)} turns Linux {@code file:///home/...} URIs into unusable paths such as
     * {@code \\home\\user\\...}. When {@code wslDistro} is set (WSL-hosted project), Linux absolute paths are mapped to
     * {@code \\wsl$\{distro}\...}, consistent with {@link com.jfrog.ide.idea.scan.ScanBinaryExecutor} binary paths.
     * Windows drive URIs ({@code file:///C:/...}) are left to the default JVM path conversion.
     */
    public static String sarifArtifactUriToLocalPath(@Nullable String uriString, @Nullable String wslDistro) {
        if (StringUtils.isBlank(uriString)) {
            return "";
        }
        URI uri;
        try {
            uri = URI.create(uriString.trim());
        } catch (IllegalArgumentException e) {
            return uriString.trim();
        }
        return sarifArtifactUriToLocalPath(uri, wslDistro);
    }

    private static String sarifArtifactUriToLocalPath(URI uri, @Nullable String wslDistro) {
        String scheme = uri.getScheme();
        if (!"file".equalsIgnoreCase(scheme != null ? scheme : "")) {
            try {
                return Paths.get(uri).toString();
            } catch (InvalidPathException e) {
                return uri.toString();
            }
        }
        if (!SystemUtils.IS_OS_WINDOWS || StringUtils.isBlank(wslDistro)) {
            try {
                return Paths.get(uri).toString();
            } catch (InvalidPathException e) {
                String p = uri.getPath();
                return p != null ? p : uri.toString();
            }
        }
        String unc = tryLinuxFileUriToWslUnc(uri, wslDistro.trim());
        if (unc != null) {
            return unc;
        }
        try {
            return Paths.get(uri).toString();
        } catch (InvalidPathException e) {
            String p = uri.getPath();
            return p != null ? p : uri.toString();
        }
    }

    /**
     * Maps {@code file:///home/...} style URIs to {@code \\wsl$\{distro}\home\...} on Windows; returns null when the URI
     * should use normal {@link Paths#get(URI)} handling (e.g. {@code file:///C:/...}).
     */
    @Nullable
    static String tryLinuxFileUriToWslUnc(URI fileUri, String wslDistro) {
        String rawPath = fileUri.getPath();
        if (rawPath == null || rawPath.isEmpty()) {
            return null;
        }
        if (isWindowsDriveFileUriPath(rawPath)) {
            return null;
        }
        if (rawPath.startsWith("/") && rawPath.length() > 1) {
            return "\\\\wsl$\\" + wslDistro + rawPath.replace('/', '\\');
        }
        return null;
    }

    /**
     * Detects {@code file} URI paths that refer to a Windows absolute path ({@code /C:/...}).
     */
    static boolean isWindowsDriveFileUriPath(String uriPath) {
        return uriPath.length() >= 4
                && uriPath.charAt(0) == '/'
                && Character.isLetter(uriPath.charAt(1))
                && uriPath.charAt(2) == ':';
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.length() >= prefix.length() && s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String toCompareKey(String path) {
        if (path == null) {
            return "";
        }
        String p = StringUtils.trimToEmpty(path);
        p = intellijWslUrlToUnc(p);
        if (WslUtils.isWslPath(p)) {
            return WslUtils.toLinuxPath(p);
        }
        try {
            p = Paths.get(p).normalize().toString();
        } catch (InvalidPathException ignored) {
            // keep p as-is for best-effort compare
        }
        p = p.replace('\\', '/');
        if (SystemUtils.IS_OS_WINDOWS) {
            p = p.toLowerCase(Locale.ROOT);
        }
        return p;
    }
}
