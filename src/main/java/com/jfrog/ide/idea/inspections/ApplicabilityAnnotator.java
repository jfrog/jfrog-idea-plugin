package com.jfrog.ide.idea.inspections;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.ApplicabilityScannerExecutor;
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
public class ApplicabilityAnnotator extends ExternalAnnotator<PsiFile, List<JfrogSecurityWarning>> {
    private final ApplicabilityScannerExecutor executor = new ApplicabilityScannerExecutor();


    @NotNull
    private static final HighlightSeverity HIGHLIGHT_TYPE = HighlightSeverity.WARNING;

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
        String projectDir = file.getProject().getBasePath();

        try {
            warnings = executor.execute(new ScanConfig.Builder()
                    .root(projectDir));
        } catch (IOException | InterruptedException | NullPointerException e) {
            logError(Logger.getInstance(), e.getMessage(), false);
        }
        return warnings;
    }

    @Override
    //
    public void apply(@NotNull PsiFile file, List<JfrogSecurityWarning> warnings, @NotNull AnnotationHolder holder) {
        var filePath = "file://" + file.getVirtualFile().getPath();
        warnings.stream().filter(warning -> warning.getFilePath().equals(filePath)).forEach(warning -> {
            int startOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineStart(), warning.getColStart());
            int endOffset = StringUtil.lineColToOffset(file.getText(), warning.getLineEnd(), warning.getColEnd());

            TextRange range = new TextRange(startOffset, endOffset);
            holder.newAnnotation(HIGHLIGHT_TYPE, "\uD83D\uDC38 JFrog: "+ warning.getReason())
                    .range(range)
                    .create();
        });
    }



}

//            PackageFileFinder packageFileFinder = new PackageFileFinder(Set.of(projectDirPath), projectDirPath,
//                    "", new NullLog());

//     .skippedFolders(packageFileFinder.getExcludedDirectories())