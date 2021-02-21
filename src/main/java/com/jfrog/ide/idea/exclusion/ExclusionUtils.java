package com.jfrog.ide.idea.exclusion;

import com.jfrog.ide.idea.navigation.NavigationTarget;
import org.jfrog.build.extractor.scan.DependencyTree;

/**
 * Created by Bar Belity on 28/05/2020.
 */
public class ExclusionUtils {

    /**
     * Check if a specific node from the Dependencies-tree can be excluded from project-descriptor.
     *
     * @param nodeToExclude - The node in tree to exclude.
     * @param affectedNode  - Direct dependency's node in tree which will be affected by the exclusion.
     * @return true if the provided nodeToExclude can be excluded from project-descriptor.
     */
    public static boolean isExcludable(DependencyTree nodeToExclude, DependencyTree affectedNode) {
        return MavenExclusion.isExcludable(nodeToExclude, affectedNode);
    }

    /**
     * Get the corresponding Excludable object for the node to exclude.
     *
     * @param nodeToExclude    - The node in tree to exclude.
     * @param navigationTarget - The navigation-target of the node to exclude.
     * @return the corresponding Excludable object, Null if exclusion is not supported for this node.
     */
    public static Excludable getExcludable(DependencyTree nodeToExclude, DependencyTree affectedNode, NavigationTarget navigationTarget) {
        if (MavenExclusion.isExcludable(nodeToExclude, affectedNode)) {
            return new MavenExclusion(nodeToExclude, navigationTarget);
        }
        return null;
    }

    /**
     * Find node's root project node.
     * In single project tree - the root's parent is null.
     * In multi project tree - the root-parent's general info is null.
     * @param node - DependencyTree node to find its project's root.
     * @return the project root node.
     */
    public static DependencyTree getProjectRoot(DependencyTree node) {
        if (node == null) {
            return null;
        }
        while (node.getParent() != null && ((DependencyTree) node.getParent()).getGeneralInfo() != null) {
            node = (DependencyTree) node.getParent();
        }
        return node;
    }
}
