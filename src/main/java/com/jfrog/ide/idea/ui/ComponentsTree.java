package com.jfrog.ide.idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.ui.components.JBMenu;
import com.intellij.ui.treeStructure.Tree;
import com.jfrog.ide.idea.exclusion.Excludable;
import com.jfrog.ide.idea.exclusion.ExclusionUtils;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.navigation.NavigationTarget;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.event.ActionEvent;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author yahavi
 */
public abstract class ComponentsTree extends Tree {
    protected Project project;
    protected final List<ToolbarPopupMenu> toolbarPopupMenus = new ArrayList<>();
    protected final JBPopupMenu popupMenu = new JBPopupMenu();

    private static final String EXCLUDE_DEPENDENCY = "Exclude dependency";


    public ComponentsTree(@NotNull Project project) {
        super((TreeModel) null);
        this.project = project;
        expandRow(0);
        setRootVisible(false);
    }

    protected void reset() {
        setModel(null);
    }

    public void addFilterMenu(ToolbarPopupMenu filterMenu) {
        this.toolbarPopupMenus.add(filterMenu);
    }


    private String getRelativizedDescriptorPath(NavigationTarget navigationTarget) {
        String pathResult = "";
        try {
            VirtualFile descriptorVirtualFile = navigationTarget.getElement().getContainingFile().getVirtualFile();
            pathResult = descriptorVirtualFile.getName();
            String projBasePath = project.getBasePath();
            if (projBasePath == null) {
                return pathResult;
            }
            Path basePath = Paths.get(project.getBasePath());
            Path descriptorPath = Paths.get(descriptorVirtualFile.getPath());
            pathResult = basePath.relativize(descriptorPath).toString();
        } catch (InvalidPathException | PsiInvalidElementAccessException ex) {
            Logger log = Logger.getInstance();
            log.error("Failed getting project-descriptor's path.", ex);
        }
        return pathResult;
    }


    private void addNodeExclusion(DependencyTree nodeToExclude, Set<NavigationTarget> parentCandidates, DependencyTree affectedNode) {
        if (parentCandidates.size() > 1) {
            addMultiExclusion(nodeToExclude, affectedNode, parentCandidates);
        } else {
            addSingleExclusion(nodeToExclude, affectedNode, parentCandidates.iterator().next());
        }
    }

    private void addMultiExclusion(DependencyTree nodeToExclude, DependencyTree affectedNode, Set<NavigationTarget> parentCandidates) {
        if (!ExclusionUtils.isExcludable(nodeToExclude, affectedNode)) {
            return;
        }
        JMenu multiMenu = new JBMenu();
        multiMenu.setText(EXCLUDE_DEPENDENCY);
        for (NavigationTarget parentCandidate : parentCandidates) {
            Excludable excludable = ExclusionUtils.getExcludable(nodeToExclude, affectedNode, parentCandidate);
            if (excludable == null) {
                continue;
            }
            String descriptorPath = getRelativizedDescriptorPath(parentCandidate);
            multiMenu.add(createExcludeMenuItem(excludable, descriptorPath + " " + (parentCandidate.getLineNumber() + 1)));
        }
        if (multiMenu.getItemCount() > 0) {
            popupMenu.add(multiMenu);
        }
    }

    private void addSingleExclusion(DependencyTree nodeToExclude, DependencyTree affectedNode, NavigationTarget parentCandidate) {
        Excludable excludable = ExclusionUtils.getExcludable(nodeToExclude, affectedNode, parentCandidate);
        if (excludable == null) {
            return;
        }
        popupMenu.add(createExcludeMenuItem(excludable, EXCLUDE_DEPENDENCY));
    }

    private JBMenuItem createExcludeMenuItem(Excludable excludable, String headLine) {
        return new JBMenuItem(new AbstractAction(headLine) {
            @Override
            public void actionPerformed(ActionEvent e) {
                excludable.exclude(project);
            }
        });
    }
}
