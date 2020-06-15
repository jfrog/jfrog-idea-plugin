package com.jfrog.ide.idea.exclusion;

import com.jfrog.ide.idea.navigation.NavigationTarget;
import org.jfrog.build.extractor.scan.DependenciesTree;

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
    public static boolean isExcludable(DependenciesTree nodeToExclude, DependenciesTree affectedNode) {
        return MavenExclusion.isExcludable(nodeToExclude, affectedNode);
    }

    /**
     * Get the corresponding Excludable object for the node to exclude.
     *
     * @param nodeToExclude    - The node in tree to exclude.
     * @param navigationTarget - The navigation-target of the node to exclude.
     * @return the corresponding Excludable object, Null if exclusion is not supported for this node.
     */
    public static Excludable getExcludable(DependenciesTree nodeToExclude, DependenciesTree affectedNode, NavigationTarget navigationTarget) {
        if (MavenExclusion.isExcludable(nodeToExclude, affectedNode)) {
            return new MavenExclusion(nodeToExclude, navigationTarget);
        }
        return null;
    }

    /**
     * Find node's root project node.
     * In single project tree - the root's parent is null.
     * In multi project tree - the root-parent's general info is null.
     * @param node - DependenciesTree node to find its project's root.
     * @return the project root node.
     */
    public static DependenciesTree getProjectRoot(DependenciesTree node) {
        if (node == null) {
            return null;
        }
        while (node.getParent() != null && ((DependenciesTree) node.getParent()).getGeneralInfo() != null) {
            node = (DependenciesTree) node.getParent();
        }
        return node;
    }
}
