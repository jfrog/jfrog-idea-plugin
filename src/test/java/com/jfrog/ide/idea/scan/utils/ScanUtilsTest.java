package com.jfrog.ide.idea.scan.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.jfrog.ide.idea.scan.utils.ScanUtils;
import org.junit.Test;


public class ScanUtilsTest {
    @Test
    public void testExtractWslDistro() {
        assertEquals("Ubuntu", ScanUtils.extractWslDistro("\\\\wsl$\\Ubuntu\\home\\user\\project"));
        assertEquals("Debian", ScanUtils.extractWslDistro("\\\\wsl.localhost\\Debian\\home\\user\\project"));
        assertEquals("Ubuntu", ScanUtils.extractWslDistro("\\\\WSL$\\Ubuntu\\home\\user"));
        // Distro with no trailing path
        assertEquals("Ubuntu", ScanUtils.extractWslDistro("\\\\wsl$\\Ubuntu"));
        // Non-WSL path returns null
        assertNull(ScanUtils.extractWslDistro("C:\\Users\\user\\project"));
        assertNull(ScanUtils.extractWslDistro(null));
    }
}