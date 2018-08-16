package org.jfrog.idea.xray.utils;

import com.google.common.collect.Sets;
import org.jfrog.idea.xray.ScanTreeNodeBase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class UtilsTests extends ScanTreeNodeBase {

    @Test(dataProvider = "Paths")
    public void testFilterProjectPaths(Set<Path> given, Set<Path> expected) {
        Set<Path> results = Utils.filterProjectPaths(given);
        assertEquals(expected.size(), results.size());
        expected.forEach(path -> assertTrue(results.contains(path)));
    }

    @DataProvider(name = "Paths")
    public static Object[][] testPreferredDeployingCredentials() {
        return new Object[][]{
                {Sets.newHashSet((Object) Paths.get("/a")), Sets.newHashSet((Object) Paths.get("/a"))},

//                {Sets.newHashSet(Paths.get("/a"), Paths.get("/b"), Paths.get("/c")),
//                        Sets.newHashSet(Paths.get("/a"), Paths.get("/b"), Paths.get("/c"))},
//
//                {Sets.newHashSet(Paths.get("/a"), Paths.get("/b/a/"), Paths.get("/c/b/d"), Paths.get("/c/b")),
//                        Sets.newHashSet(Paths.get("/a"), Paths.get("/b/a/"), Paths.get("/c/b"))},
//
//                {Sets.newHashSet(Paths.get("/a"), Paths.get("/a/b"), Paths.get("/c/b")),
//                        Sets.newHashSet(Paths.get("/a"), Paths.get("/c/b"))},
//
//                {Sets.newHashSet(Paths.get("/a"), Paths.get("/a/a"), Paths.get("/a/b")),
//                        Sets.newHashSet((Object) Paths.get("/a"))},
//
//                {Sets.newHashSet(Paths.get("/a"), Paths.get("/a/a/"), Paths.get("/c/b"), Paths.get("/c/b/fff"), Paths.get("/c/f/fff")),
//                        Sets.newHashSet(Paths.get("/a"), Paths.get("/c/b"), Paths.get("/c/f/fff"))},
//
//                {Sets.newHashSet(Paths.get("/a"), Paths.get("/a/a/"), Paths.get("/c/b"), Paths.get("/c/b/../b/../b/fff"), Paths.get("/c/f/fff")),
//                        Sets.newHashSet(Paths.get("/a"), Paths.get("/c/b"), Paths.get("/c/f/fff"))},
//
//                {Sets.newHashSet(Paths.get("/a"), Paths.get("/a/a/"), Paths.get("/c/b/.."), Paths.get("/c/b/../b/../b/fff"), Paths.get("/c/f/fff")),
//                        Sets.newHashSet(Paths.get("/a"), Paths.get("/c"))},
        };
    }
}
