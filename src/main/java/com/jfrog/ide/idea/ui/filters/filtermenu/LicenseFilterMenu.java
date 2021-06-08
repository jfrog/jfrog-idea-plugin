package com.jfrog.ide.idea.ui.filters.filtermenu;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.License;

/**
 * Created by Yahav Itzhak on 23 Nov 2017.
 */
public abstract class LicenseFilterMenu extends FilterMenu<License> {

    public static final String NAME = "License";
    public static final String TOOLTIP = "Select licenses to show";

    public LicenseFilterMenu(@NotNull Project project) {
        super(project, NAME, TOOLTIP);
    }
}