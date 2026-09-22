package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.gradle.GradleTreeBuilder;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.NullLog;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XRAY-100231 - a Gradle multi-module project where the same dependency resolves with different
 * transitive dependencies in two modules loses the transitive dependency's parent edge, which makes
 * the impact graph builder throw a NullPointerException.
 */
public class GradleOrphanImpactTreeTest {
    private static final String ORPHAN_COMP_ID = "org.apache.commons:commons-lang3:3.11";

    @Test
    public void testImpactTreeOfDependencyDroppedByModuleMerge() throws Exception {
        DepTree depTree = buildOrphanProjectTree();
        Assert.assertTrue("commons-lang3 is expected in the dependency tree", depTree.nodes().containsKey(ORPHAN_COMP_ID));

        DependencyNode vulnerableDependency = new DependencyNode().componentId("gav://" + ORPHAN_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(ORPHAN_COMP_ID, vulnerableDependency), depTree);

        Assert.assertNotNull("an impact path should have been built for commons-lang3", vulnerableDependency.getImpactTree());
        List<String> paths = new ArrayList<>();
        collectPaths(vulnerableDependency.getImpactTree().getRoot(), "", paths);
        Assert.assertEquals("commons-lang3 is only reachable through moda: " + paths, 1, paths.size());
        Assert.assertTrue("the only impact path must go through moda: " + paths, paths.get(0).contains(":moda:"));
        Assert.assertFalse("modb excludes commons-lang3: " + paths, paths.get(0).contains(":modb:"));
    }

    private void collectPaths(ImpactTreeNode node, String prefix, List<String> paths) {
        String path = prefix.isEmpty() ? node.getName() : prefix + " -> " + node.getName();
        if (node.getChildren().isEmpty()) {
            paths.add(path);
            return;
        }
        node.getChildren().forEach(child -> collectPaths(child, path, paths));
    }

    private DepTree buildOrphanProjectTree() throws Exception {
        Path source = Paths.get("src", "test", "resources", "gradle", "orphan").toAbsolutePath().normalize();
        Path projectDir = Files.createTempDirectory("orphan");
        FileUtils.copyDirectory(source.toFile(), projectDir.toFile());
        File gradleExe = projectDir.resolve(SystemUtils.IS_OS_WINDOWS ? "gradlew.bat" : "gradlew").toFile();
        Assert.assertTrue(gradleExe.setExecutable(true));
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("pluginLibDir", copyDepTreePlugin(projectDir).toString());
        GradleTreeBuilder treeBuilder = new GradleTreeBuilder(projectDir, projectDir.resolve("build.gradle").toString(), env, gradleExe.getPath());
        return treeBuilder.buildTree(new NullLog());
    }

    private Path copyDepTreePlugin(Path projectDir) throws Exception {
        Path libDir = Files.createDirectory(projectDir.resolve("gradle-dep-tree-lib"));
        for (String jar : System.getProperty("gradleDepTreeLib").split(File.pathSeparator)) {
            FileUtils.copyFileToDirectory(new File(jar), libDir.toFile());
        }
        return libDir;
    }
}
