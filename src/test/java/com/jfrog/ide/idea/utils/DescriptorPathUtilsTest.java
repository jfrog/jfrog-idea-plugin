package com.jfrog.ide.idea.utils;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class DescriptorPathUtilsTest {

    private static final Object[][] INTELLIJ_WSL_URL_TO_UNC_CASES = new Object[][]{
            {"//wsl$/Ubuntu/home/user/app", "\\\\wsl$\\Ubuntu\\home\\user\\app"},
            {"//WSL$/Ubuntu/home/user/app", "\\\\wsl$\\Ubuntu\\home\\user\\app"},
            {"//wsl.localhost/Ubuntu/home/user/app", "\\\\wsl.localhost\\Ubuntu\\home\\user\\app"},
            {"//WSL.LOCALHOST/Ubuntu/home/user/app", "\\\\wsl.localhost\\Ubuntu\\home\\user\\app"},
            {"C:\\plain\\path", "C:\\plain\\path"},
    };

    @Test
    public void testIntellijWslUrlToUnc() {
        for (Object[] row : INTELLIJ_WSL_URL_TO_UNC_CASES) {
            String input = (String) row[0];
            String expected = (String) row[1];
            assertEquals(expected, DescriptorPathUtils.intellijWslUrlToUnc(input));
        }
    }

    @Test
    public void testAreDescriptorPathsEqual_uncAndIntellijUrl() {
        String unc = "\\\\wsl$\\Ubuntu\\home\\user\\repo\\package.json";
        String intellij = "//wsl$/Ubuntu/home/user/repo/package.json";
        assertTrue(DescriptorPathUtils.areDescriptorPathsEqual(unc, intellij));
    }

    @Test
    public void testAreDescriptorPathsEqual_uncVariants() {
        String a = "\\\\wsl$\\Ubuntu\\home\\user\\app";
        String b = "\\\\WSL$\\Ubuntu\\home\\user\\app";
        assertTrue(DescriptorPathUtils.areDescriptorPathsEqual(a, b));
    }

    @Test
    public void testAreDescriptorPathsEqual_sameLinuxPathInsideWsl() {
        String unc = "\\\\wsl$\\Ubuntu\\home\\user\\proj";
        String intellij = "//wsl$/Ubuntu/home/user/proj";
        assertTrue(DescriptorPathUtils.areDescriptorPathsEqual(unc, intellij));
    }

    @Test
    public void testAreDescriptorPathsEqual_extendedLengthUnc() {
        String extended = "\\\\?\\UNC\\wsl$\\Ubuntu\\home\\user\\app";
        String regular = "\\\\wsl$\\Ubuntu\\home\\user\\app";
        assertTrue(DescriptorPathUtils.areDescriptorPathsEqual(extended, regular));
    }

    @Test
    public void testAreDescriptorPathsEqual_nullAndIdentical() {
        assertTrue(DescriptorPathUtils.areDescriptorPathsEqual(null, null));
        assertTrue(DescriptorPathUtils.areDescriptorPathsEqual("/a/b", "/a/b"));
    }

    @Test
    public void tryLinuxFileUriToWslUnc_mapsLinuxRootToUnc() {
        assertEquals(
                "\\\\wsl$\\Ubuntu\\home\\user\\app.js",
                DescriptorPathUtils.tryLinuxFileUriToWslUnc(URI.create("file:///home/user/app.js"), "Ubuntu"));
    }

    @Test
    public void tryLinuxFileUriToWslUnc_skipsWindowsDriveUri() {
        assertNull(DescriptorPathUtils.tryLinuxFileUriToWslUnc(URI.create("file:///C:/Users/dev/project"), "Ubuntu"));
    }

    @Test
    public void isWindowsDriveFileUriPath_detectsDriveLetter() {
        assertTrue(DescriptorPathUtils.isWindowsDriveFileUriPath("/C:/Users/dev"));
        assertTrue(DescriptorPathUtils.isWindowsDriveFileUriPath("/c:/Users/dev"));
        assertFalse(DescriptorPathUtils.isWindowsDriveFileUriPath("/home/user"));
    }

    @Test
    public void sarifArtifactUriToLocalPath_windowsMapsLinuxFileUriToUnc() {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        assertEquals(
                "\\\\wsl$\\Ubuntu\\home\\user\\repo\\app.js",
                DescriptorPathUtils.sarifArtifactUriToLocalPath("file:///home/user/repo/app.js", "Ubuntu"));
    }

    @Test
    public void sarifArtifactUriToLocalPath_windowsLeavesWindowsDriveUriUnchangedStyle() {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        String resolved = DescriptorPathUtils.sarifArtifactUriToLocalPath("file:///C:/Users/dev/x.txt", "Ubuntu");
        assertTrue(resolved.endsWith("x.txt"));
        assertFalse(resolved.startsWith("\\\\wsl$\\"));
    }

    @Test
    public void sarifArtifactUriToLocalPath_nonWindowsUsesJvmPathForLinuxUri() {
        assumeTrue(!SystemUtils.IS_OS_WINDOWS);
        assertEquals(
                "/home/user/app.js",
                DescriptorPathUtils.sarifArtifactUriToLocalPath("file:///home/user/app.js", "Ubuntu"));
    }

    @Test
    public void sarifArtifactUriToLocalPath_blankReturnsEmpty() {
        assertEquals("", DescriptorPathUtils.sarifArtifactUriToLocalPath("", null));
        assertEquals("", DescriptorPathUtils.sarifArtifactUriToLocalPath("   ", null));
    }
}
