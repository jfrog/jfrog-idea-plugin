package com.jfrog.ide.idea.scan;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.jfrog.ide.idea.scan.SourceCodeScannerManager.convertToSkippedFolders;

public class SourceCodeManagerTest {

    @RunWith(Parameterized.class)
    public static class ConvertToSkipFoldersTest {

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"", new String[]{}},
                    {"**/*{.idea}*", new String[]{"**/*.idea*/**"}},
                    {"**/*.idea*", new String[]{"**/*.idea*/**"}},
                    {"**/*{.idea,test,node_modules}*", new String[]{"**/*.idea*/**", "**/*test*/**", "**/*node_modules*/**"}},
            });
        }

        private final String excludedPaths;
        private final String[] skipFolders;

        public ConvertToSkipFoldersTest(String excludedPaths, String[] skipFolders) {
            this.excludedPaths = excludedPaths;
            this.skipFolders = skipFolders;
        }

        @Test
        public void testConvertToSkippedFolders() {
            Assert.assertArrayEquals(skipFolders, convertToSkippedFolders(excludedPaths).toArray());
        }
    }
}
