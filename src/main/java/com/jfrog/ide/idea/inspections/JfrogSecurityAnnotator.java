package com.jfrog.ide.idea.inspections;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tal Arian
 */
public class JfrogSecurityAnnotator extends ExternalAnnotator<PsiFile, List<JfrogSecurityWarning>> {

    @NotNull
    private static final HighlightSeverity HIGHLIGHT_TYPE = HighlightSeverity.WARNING;

    @Nullable
    @Override
    public PsiFile collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return file;
    }

    @Nullable
    @Override
    public List<JfrogSecurityWarning> doAnnotate(PsiFile file) {
        var scanManager = ScanManagersFactory.getScanManagers(file.getProject()).stream()
                .findAny()
                .orElse(null);
        return scanManager != null ? scanManager.getSourceCodeScanResults() : new ArrayList<>();
    }

    @Override
    //
    public void apply(@NotNull PsiFile file, List<JfrogSecurityWarning> warnings, @NotNull AnnotationHolder holder) {
        var filePath = file.getVirtualFile().getPath();
        warnings.stream().filter(warning -> warning.getFilePath().equals(filePath)).forEach(warning -> {
            int startOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineStart(), warning.getColStart());
            int endOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineEnd(), warning.getColEnd());

            TextRange range = new TextRange(startOffset, endOffset);
            holder.newAnnotation(HIGHLIGHT_TYPE, "\uD83D\uDC38 JFrog [" + warning.getName() + "]: " + warning.getReason())
                    .range(range)
                    .create();
        });
    }


}
