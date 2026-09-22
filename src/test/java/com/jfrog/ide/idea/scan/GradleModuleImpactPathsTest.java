package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.gradle.GradleTreeBuilder;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.NullLog;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Gradle project whose modules resolve the same dependency differently: 'moda' pulls commons-lang3
 * in through commons-text, and 'modb' excludes it. The impact paths of commons-lang3 must therefore
 * name 'moda' alone.
 */
public class GradleModuleImpactPathsTest {
    private static final String EXCLUDED_BY_MODB_COMP_ID = "org.apache.commons:commons-lang3:3.11";
    private static final String GRADLE_DEP_TREE_CLASS = "com.jfrog.GradleDependencyNode";

    @Test
    public void testImpactPathsNameOnlyTheModuleThatResolvesTheDependency() throws Exception {
        Path projectDir = Files.createTempDirectory("moduleImpactPaths");
        try {
            DepTree depTree = buildDependencyTree(projectDir);
            Assert.assertTrue("commons-lang3 is expected in the dependency tree", depTree.nodes().containsKey(EXCLUDED_BY_MODB_COMP_ID));

            DependencyNode vulnerableDependency = new DependencyNode().componentId("gav://" + EXCLUDED_BY_MODB_COMP_ID);
            ScannerBase.populateImpactTrees(Map.of(EXCLUDED_BY_MODB_COMP_ID, vulnerableDependency), depTree);

            Assert.assertNotNull("an impact path should have been built for commons-lang3", vulnerableDependency.getImpactTree());
            List<String> paths = new ArrayList<>();
            collectPaths(vulnerableDependency.getImpactTree().getRoot(), "", paths);
            Assert.assertEquals("commons-lang3 is only reachable through moda: " + paths, 1, paths.size());
            Assert.assertTrue("the only impact path must go through moda: " + paths, paths.get(0).contains(":moda:"));
            Assert.assertFalse("modb excludes commons-lang3: " + paths, paths.get(0).contains(":modb:"));
        } finally {
            FileUtils.deleteQuietly(projectDir.toFile());
        }
    }

    private void collectPaths(ImpactTreeNode node, String prefix, List<String> paths) {
        String path = prefix.isEmpty() ? node.getName() : prefix + " -> " + node.getName();
        if (node.getChildren().isEmpty()) {
            paths.add(path);
            return;
        }
        node.getChildren().forEach(child -> collectPaths(child, path, paths));
    }

    private DepTree buildDependencyTree(Path projectDir) throws Exception {
        Path source = Paths.get("src", "test", "resources", "gradle", "moduleImpactPaths").toAbsolutePath().normalize();
        FileUtils.copyDirectory(source.toFile(), projectDir.toFile());
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("pluginLibDir", copyDepTreePlugin(projectDir).toString());
        GradleTreeBuilder treeBuilder = new GradleTreeBuilder(projectDir, projectDir.resolve("build.gradle").toString(), env, "");
        return treeBuilder.buildTree(new NullLog());
    }

    private Path copyDepTreePlugin(Path projectDir) throws Exception {
        Path libDir = Files.createDirectories(projectDir.resolve("gradle-dep-tree-lib"));
        FileUtils.copyFileToDirectory(gradleDepTreeJar().toFile(), libDir.toFile());
        return libDir;
    }

    private Path gradleDepTreeJar() throws Exception {
        String resource = "/" + GRADLE_DEP_TREE_CLASS.replace('.', '/') + ".class";
        URL location = Class.forName(GRADLE_DEP_TREE_CLASS).getResource(resource);
        Assert.assertTrue("'" + GRADLE_DEP_TREE_CLASS + "' is expected to come from a jar, but was loaded from " + location,
                StringUtils.startsWith(location.toString(), "jar:") && StringUtils.contains(location.toString(), "!"));
        String jarUrl = StringUtils.substringBefore(StringUtils.removeStart(location.toString(), "jar:"), "!");
        return Path.of(URI.create(jarUrl));
    }
}
