package com.jfrog.ide.idea.inspections;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiType;
import com.jfrog.ide.idea.scan.GradleScanManager;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
@SuppressWarnings("InspectionDescriptionNotFoundInspection")
public class GradleInspection extends AbstractInspection {

    public static final String GRADLE_VERSION_KEY = "version";
    public static final String GRADLE_GROUP_KEY = "group";
    public static final String GRADLE_NAME_KEY = "name";

    public GradleInspection() {
        super("build.gradle");
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new GroovyPsiElementVisitor(new GroovyElementVisitor() {
            @Override
            public void visitLiteralExpression(@NotNull GrLiteral literal) {
                super.visitLiteralExpression(literal);
                GradleInspection.this.visitElement(holder, literal);
            }
        });
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof GrLiteral) {
            GradleInspection.this.visitElement(holder, element);
        }
    }

    @Override
    PsiElement[] getTargetElements(PsiElement element) {
        if (element.getParent() instanceof GrNamedArgument) {
            return new PsiElement[]{element.getParent()};
        }
        return new PsiElement[]{element};
    }

    @Override
    ScanManager getScanManager(Project project, String path) {
        return ScanManagersFactory.getScanManagers(project).stream()
                .filter(GradleScanManager.class::isInstance)
                .findAny()
                .orElse(null);
    }

    @Override
    boolean isDependency(PsiElement element) {
        PsiElement parent = element.getParent();
        for (int i = 0; i < 4; i++, parent = parent.getParent()) {
            if (parent instanceof GrExpression) {
                PsiType type = ((GrExpression) parent).getType();
                if (type != null && "Dependency".equals(type.getPresentableText())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    GeneralInfo createGeneralInfo(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof GrNamedArgument) {
            GrNamedArgumentsOwner namedArgument = (GrNamedArgumentsOwner) parent.getParent();
            return new GeneralInfo()
                    .groupId(extractExpresion(namedArgument, GRADLE_GROUP_KEY))
                    .artifactId(extractExpresion(namedArgument, GRADLE_NAME_KEY))
                    .version(extractExpresion(namedArgument, GRADLE_VERSION_KEY));
        }
        String componentId = getLiteralValue((GrLiteral) element);
        if (componentId.startsWith(":")) { // compile project(':xyz')
            componentId = componentId + ":";
        }
        return new GeneralInfo().componentId(componentId);
    }

    @Override
    Set<DependenciesTree> getModules(PsiElement element, GeneralInfo generalInfo) {
        Project project = element.getProject();
        DependenciesTree root = getRootDependenciesTree(element);
        List<String> gradleModules = getGradleModules(project);
        if (root == null || gradleModules == null) {
            return null;
        }

        // Single project, single module
        if (gradleModules.size() <= 1 && root.getGeneralInfo() != null) {
            return Sets.newHashSet(root);
        }

        // Multi project
        root = getProjectNode(root, project);

        // Multi module
        if (isModule(root, generalInfo)) {
            return Sets.newHashSet(root);
        }

        // Collect the modules containing the dependency
        return collectModules(root, generalInfo);
    }

    /**
     * Get all modules of the current project
     *
     * @param project - The current project
     * @return list of gradle modules or null if the Gradle project not yet initialized
     */
    private List<String> getGradleModules(Project project) {
        GradleSettings.MyState gradleState = GradleSettings.getInstance(project).getState();
        if (gradleState == null) {
            return null;
        }
        return gradleState.getLinkedExternalProjectsSettings().stream()
                .map(ExternalProjectSettings::getModules)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Return true iff the dependency stated in the General info is a module in the project.
     *
     * @param node        - The project node
     * @param generalInfo - General info of the dependency
     * @return true iff the dependency stated in the General info is a module in the project
     */
    private boolean isModule(DependenciesTree node, GeneralInfo generalInfo) {
        return node.getChildren().stream()
                .map(DefaultMutableTreeNode::getUserObject)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .anyMatch(name -> name.equals(generalInfo.getArtifactId()));
    }

    /**
     * Collect all modules containing the dependency stated in the general info.
     *
     * @param node        - The project node
     * @param generalInfo - General info of the dependency
     * @return list of all nodes of the Gradle modules
     */
    private Set<DependenciesTree> collectModules(DependenciesTree node, GeneralInfo generalInfo) {
        Set<DependenciesTree> modules = Sets.newHashSet();
        node.getChildren().forEach(child -> child.getChildren().stream()
                .map(DependenciesTree::getGeneralInfo)
                .filter(Objects::nonNull)
                .filter(grandChildGeneralInfo -> Utils.gavStringEqual(grandChildGeneralInfo, generalInfo))
                .findAny()
                .ifPresent(grandChildGeneralInfo -> modules.add(child)));
        return modules;
    }

    /**
     * Extract expression from groovy arguments.
     *
     * @param argumentList - The arguments list
     * @param name         - The name of the argument to extract
     * @return the value of the argument
     */
    private String extractExpresion(GrNamedArgumentsOwner argumentList, String name) {
        GrNamedArgument argument = argumentList.findNamedArgument(name);
        if (argument == null) {
            return "";
        }
        GrExpression grExpression = argument.getExpression();
        if (grExpression == null) {
            return "";
        }
        if (!(grExpression instanceof GrLiteral)) {
            return "";
        }
        return getLiteralValue((GrLiteral) grExpression);
    }

    /**
     * Get the string value of the groovy literal.
     *
     * @param literal - The groovy literal
     * @return the value of the literal
     */
    private String getLiteralValue(GrLiteral literal) {
        return Objects.toString((literal).getValue(), "");
    }
}
