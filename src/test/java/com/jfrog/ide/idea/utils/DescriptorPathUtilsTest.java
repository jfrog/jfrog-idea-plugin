package com.jfrog.ide.idea.utils;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DescriptorPathUtilsTest {

    @DataProvider
    public Object[][] intellijWslUrlToUnc() {
        return new Object[][]{
                {"//wsl$/Ubuntu/home/user/app", "\\\\wsl$\\Ubuntu\\home\\user\\app"},
                {"//WSL$/Ubuntu/home/user/app", "\\\\wsl$\\Ubuntu\\home\\user\\app"},
                {"//wsl.localhost/Ubuntu/home/user/app", "\\\\wsl.localhost\\Ubuntu\\home\\user\\app"},
                {"//WSL.LOCALHOST/Ubuntu/home/user/app", "\\\\wsl.localhost\\Ubuntu\\home\\user\\app"},
                {"C:\\plain\\path", "C:\\plain\\path"},
        };
    }

    @Test(dataProvider = "intellijWslUrlToUnc")
    public void testIntellijWslUrlToUnc(String input, String expected) {
        assertEquals(DescriptorPathUtils.intellijWslUrlToUnc(input), expected);
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
}
