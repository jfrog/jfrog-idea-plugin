package com.jfrog.ide.idea.ui.webview.event.tasks;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.inspections.JumpToCode;
import com.jfrog.ide.idea.ui.webview.model.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a task that performs the "Jump to Code" action in the IDE.
 * This task is responsible for executing the jump to code operation based on the provided project and location.
 */
public class JumpToCodeTask {
    JumpToCode jumpToCode;

    public JumpToCodeTask(@NotNull Project project) {
        jumpToCode = JumpToCode.getInstance(project);
    }

    public void execute(Location location) {
        this.jumpToCode.execute(location.getFile(), location.getStartRow() - 1, location.getEndRow() - 1, location.getStartColumn() - 1, location.getEndColumn() - 1);
    }
}