package com.jfrog.ide.idea.inspections;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jfrog.ide.common.log.Utils.logError;

/**
 * @author Tal Arian
 */
public class Eos extends ExternalAnnotator<PsiFile, List<JfrogSecurityWarning>> {
    private final com.jfrog.ide.idea.scan.Eos executor = new com.jfrog.ide.idea.scan.Eos();


    @NotNull
    private static final HighlightSeverity HIGHLIGHT_TYPE = HighlightSeverity.WEAK_WARNING;

    @Nullable
    @Override
    public PsiFile collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        //
        return file;
    }

    @Nullable
    @Override
    public List<JfrogSecurityWarning> doAnnotate(PsiFile file) {
        List<JfrogSecurityWarning> warnings = new ArrayList<>();
        try {
            warnings = executor.execute(new ScanConfig.Builder()
                    .language(file.getFileElementType().getLanguage().toString().toLowerCase())
                    .roots(List.of(file.getProject().getBasePath())));
        } catch (IOException | InterruptedException | NullPointerException e) {
            logError(Logger.getInstance(), e.getMessage(), false);
        }
        return warnings;
    }

    @Override
    public void apply(@NotNull PsiFile file, List<JfrogSecurityWarning> warnings, @NotNull AnnotationHolder holder) {
        warnings.stream().filter(warning -> warning.getFilePath().equals(file.getVirtualFile().getPath())).forEach(warning -> {
            int startOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineStart(), warning.getColEnd());
            int endOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineEnd(), warning.getColStart());

            TextRange range = new TextRange(startOffset, endOffset);
            holder.newAnnotation(HIGHLIGHT_TYPE, warning.getReason())
                    .range(range)
                    .create();
        });
    }

}
