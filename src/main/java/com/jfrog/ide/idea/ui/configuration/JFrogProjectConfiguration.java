package com.jfrog.ide.idea.ui.configuration;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.messages.MessageBus;
import com.jfrog.ide.idea.events.ApplicationEvents;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * @author yahavi
 **/
public class JFrogProjectConfiguration implements Configurable, Configurable.NoScroll {
    public static final String BUILDS_PATTERN_KEY = "buildsPattern";
    private JBTextField buildsPattern;
    private final Project project;
    private JPanel config;

    public JFrogProjectConfiguration(Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "CI Integration";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return config;
    }

    @Override
    public boolean isModified() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        return !StringUtils.equals(propertiesComponent.getValue(BUILDS_PATTERN_KEY), buildsPattern.getText());
    }

    @Override
    public void apply() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        propertiesComponent.setValue(BUILDS_PATTERN_KEY, trim(buildsPattern.getText()));
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(ApplicationEvents.ON_BUILDS_CONFIGURATION_CHANGE).update();
    }

    @Override
    public void reset() {
        loadConfig();
    }

    private void loadConfig() {
        buildsPattern.getEmptyText().setText("Example: my-build-*");
        buildsPattern.setInputVerifier(new BuildsVerifier(buildsPattern));
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String buildPattern = propertiesComponent.getValue(BUILDS_PATTERN_KEY);
        if (buildPattern != null) {
            buildsPattern.setText(buildPattern);
        }
    }
}
