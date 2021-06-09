package com.jfrog.ide.idea.ui.filters.filtermenu;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Scope;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public abstract class ScopeFilterMenu extends FilterMenu<Scope> {

    public static final String NAME = "Scope";
    public static final String TOOLTIP = "Select scopes to show";

    public ScopeFilterMenu(@NotNull Project project) {
        super(project, NAME, TOOLTIP);
    }

}