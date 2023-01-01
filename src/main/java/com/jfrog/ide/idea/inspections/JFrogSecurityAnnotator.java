package com.jfrog.ide.idea.inspections;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * General Annotator for JFrog security source code issues (for Example: applicable CVE)
 * The annotator receives the JFrogSecurityWarning data from the most recent scan.
 *
 * @author Tal Arian
 */
public class JFrogSecurityAnnotator extends ExternalAnnotator<PsiFile, List<JFrogSecurityWarning>> {

    @NotNull
    private static final HighlightSeverity HIGHLIGHT_TYPE = HighlightSeverity.WARNING;

    @Nullable
    @Override
    public PsiFile collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return file;
    }

    @Nullable
    @Override
    public List<JFrogSecurityWarning> doAnnotate(PsiFile file) {
        ScanManager scanManager = ScanManagersFactory.getScanManagers(file.getProject()).stream()
                .findAny()
                .orElse(null);
        return scanManager != null ? scanManager.getSourceCodeScanResults() : new ArrayList<>();
    }

    @Override
    public void apply(@NotNull PsiFile file, List<JFrogSecurityWarning> warnings, @NotNull AnnotationHolder holder) {
        String filePath = file.getVirtualFile().getPath();
        warnings.stream().filter(warning -> warning.getFilePath().equals(filePath)).forEach(warning -> {
            int startOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineStart(), warning.getColStart());
            int endOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineEnd(), warning.getColEnd());

            TextRange range = new TextRange(startOffset, endOffset);
            Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
            if (document != null) {
                String lineText = document.getText(range);
                // If the file has been update after the scan and the relevant line is affected,
                // no annotation will be added.
                if (lineText.contains(warning.getLineSnippet())) {
                    holder.newAnnotation(HIGHLIGHT_TYPE, "\uD83D\uDC38 JFrog [" + warning.getName() + "]: " + warning.getReason())
                            .range(range)
                            .create();
                }
            }
        });
    }
}
