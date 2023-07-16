package com.jfrog.ide.idea.inspections;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

/**
 * The JumpToCode class is responsible for navigating to a specific location in a code file
 * and highlighting the corresponding code.
 */
public class JumpToCode {
    Project project;
    FileEditorManager fileEditorManager;

    /**
     * Constructs a new {@code JumpToCode} with the provided project.
     *
     * @param project The current project.
     */
    public JumpToCode(@NotNull Project project) {
        this.project = project;
        fileEditorManager = FileEditorManager.getInstance(project);
    }

    /**
     * Executes the jump to code operation by opening the file in the editor and highlighting the specified code range.
     *
     * @param filePath    The path of the file to navigate to.
     * @param startRow    The starting row of the code range.
     * @param endRow      The ending row of the code range.
     * @param startColumn The starting column of the code range.
     * @param endColumn   The ending column of the code range.
     */
    public void execute(String filePath, int startRow, int endRow, int startColumn, int endColumn) {
        if (this.project == null || this.fileEditorManager == null) return;
        VirtualFile file = getVirtualFile(filePath);
        if (file == null) return;
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                openFileInEditor(file);
                highlightCode(startRow, endRow, startColumn, endColumn);

            });
        });
    }

    private void openFileInEditor(VirtualFile file) {
        fileEditorManager.openFile(file, true);
    }

    private void highlightCode(int startRow, int endRow, int startColumn, int endColumn) {
        Editor editor = fileEditorManager.getSelectedTextEditor();
        if (editor == null) return;
        Document document = getDocument(editor);
        if (document == null) return;
        int startOffset = getOffset(document, startRow - 1, startColumn - 1);
        int endOffset = getOffset(document, endRow - 1, endColumn - 1);
        highlightCode(editor, startOffset, endOffset);
        scrollToHighlightedCode(editor, startOffset);
    }

    private VirtualFile getVirtualFile(String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    private Document getDocument(Editor editor) {
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) return null;
        return psiFile.getViewProvider().getDocument();
    }

    private int getOffset(Document document, int row, int column) {
        return StringUtil.lineColToOffset(document.getText(), row, column);
    }

    private void highlightCode(Editor editor, int startOffset, int endOffset) {
        SelectionModel selectionModel = editor.getSelectionModel();
        selectionModel.setSelection(startOffset, endOffset);
        editor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, 0, null, HighlighterTargetArea.EXACT_RANGE);
    }

    private void scrollToHighlightedCode(Editor editor, int startOffset) {
        editor.getCaretModel().moveToOffset(startOffset);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }
}