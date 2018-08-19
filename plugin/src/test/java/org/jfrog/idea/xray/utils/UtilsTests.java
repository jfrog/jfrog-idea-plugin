package org.jfrog.idea.xray.utils;

import com.google.common.collect.Sets;
import org.jfrog.idea.xray.ScanTreeNodeBase;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class UtilsTests extends ScanTreeNodeBase {

    private static String rootFolderA, rootFolderB, rootFolderC;

    @BeforeTest
    public void init() {
        if (Utils.isWindows()) {
            rootFolderA = "C:\\";
            rootFolderB = "D:\\";
            rootFolderC = "E:\\";
        } else {
            rootFolderA = "/a";
            rootFolderB = "/b";
            rootFolderC = "/c";
        }
    }

    @Test(dataProvider = "Paths")
    public void testFilterProjectPaths(Set<Path> given, Set<Path> expected) {
        Set<Path> results = Utils.filterProjectPaths(given);
        assertEquals(expected.size(), results.size());
        expected.forEach(path -> assertTrue(results.contains(path)));
    }

    @DataProvider(name = "Paths")
    public static Object[][] testPreferredDeployingCredentials() {
        return new Object[][]{
                {Sets.newHashSet((Object) Paths.get(rootFolderA)),
                        Sets.newHashSet((Object) Paths.get(rootFolderA))},

                {Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderB), Paths.get(rootFolderC)),
                        Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderB), Paths.get(rootFolderC))},

                {Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderB, "a"), Paths.get(rootFolderC, "b", "d"), Paths.get(rootFolderC, "b")),
                        Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderB, "a"), Paths.get(rootFolderC, "b"))},

                {Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderA, "b"), Paths.get(rootFolderC, "b")),
                        Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderC , "b"))},

                {Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderA ,"a"), Paths.get(rootFolderA, "b")),
                        Sets.newHashSet((Object) Paths.get(rootFolderA))},

                {Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderA, "a"), Paths.get(rootFolderC ,"b"), Paths.get(rootFolderC, "b", "fff"), Paths.get(rootFolderC, "f", "fff")),
                        Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderC, "b"), Paths.get(rootFolderC, "f", "fff"))},

                {Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderA, "a/"), Paths.get(rootFolderC, "b"), Paths.get(rootFolderC, "b", "..", "b", "..", "b", "fff"), Paths.get(rootFolderC, "f", "fff")),
                        Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderC, "b"), Paths.get(rootFolderC, "f", "fff"))},

                {Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderA, "a"), Paths.get(rootFolderC, "b", ".."), Paths.get(rootFolderC, "b", "..", "b", "..", "b", "fff"), Paths.get(rootFolderC, "f", "fff")),
                        Sets.newHashSet(Paths.get(rootFolderA), Paths.get(rootFolderC))},
        };
    }
}
