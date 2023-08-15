package com.jfrog.ide.idea.inspections;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.MessageBus;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.SortableChildrenTreeNode;
import com.jfrog.ide.idea.events.AnnotationEvents;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * General Annotator for JFrog security source code issues (for Example: applicable CVE)
 * The annotator receives the JFrogSecurityWarning data from the most recent scan.
 *
 * @author Tal Arian
 */
public class JFrogSecurityAnnotator extends ExternalAnnotator<PsiFile, List<FileIssueNode>> {

    @NotNull
    private static final HighlightSeverity HIGHLIGHT_TYPE = HighlightSeverity.WARNING;

    @Nullable
    @Override
    public PsiFile collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return file;
    }

    @Nullable
    @Override
    public List<FileIssueNode> doAnnotate(PsiFile file) {
        List<FileIssueNode> issues = new ArrayList<>();
        ComponentsTree componentsTree = LocalComponentsTree.getInstance(file.getProject());
        if (componentsTree == null || componentsTree.getModel() == null) {
            return null;
        }
        Enumeration<TreeNode> roots = ((SortableChildrenTreeNode) componentsTree.getModel().getRoot()).children();
        roots.asIterator().forEachRemaining(root -> {
            FileTreeNode fileNode = (FileTreeNode) root;
            if (fileNode.getFilePath().equals(file.getContainingFile().getVirtualFile().getPath())) {
                fileNode.children().asIterator().forEachRemaining(issueNode -> {
                            if (issueNode instanceof FileIssueNode) {
                                issues.add((FileIssueNode) issueNode);
                            }
                        }
                );
            }
        });
        return issues;
    }

    @Override
    public void apply(@NotNull PsiFile file, List<FileIssueNode> warnings, @NotNull AnnotationHolder holder) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return;
        }
        warnings.stream().filter(Objects::nonNull).forEach(warning -> {
            int startOffset = StringUtil.lineColToOffset(file.getText(), warning.getRowStart(), warning.getColStart());
            int endOffset = StringUtil.lineColToOffset(file.getText(), warning.getRowEnd(), warning.getColEnd());
            AnnotationIconRenderer iconRenderer = new AnnotationIconRenderer(warning, warning.getSeverity(), "");
            iconRenderer.setProject(file.getProject());

            TextRange range = new TextRange(startOffset, endOffset);
            String lineText = document.getText(range);
            // If the file has been update after the scan and the relevant line is affected,
            // no annotation will be added.
            if (lineText.contains(warning.getLineSnippet())) {
                holder.newAnnotation(HIGHLIGHT_TYPE, "\uD83D\uDC38 JFrog [" + warning.getTitle() + "]: " + warning.getReason())
                        .range(range)
                        .gutterIconRenderer(iconRenderer)
                        .create();
            }
            // Notify outdated scan result
            else {
                MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
                messageBus.syncPublisher(AnnotationEvents.ON_IRRELEVANT_RESULT).update(warning.getFilePath());
            }
        });
    }
}
