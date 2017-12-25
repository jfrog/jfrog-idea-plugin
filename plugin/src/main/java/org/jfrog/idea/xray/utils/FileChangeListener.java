package org.jfrog.idea.xray.utils;


import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class FileChangeListener implements VirtualFileListener {

    public enum FileEventType {Create, Delete, Move, Copy, Change}

    public interface Callback {
        void run(@NotNull VirtualFileEvent virtualFileEvent, FileEventType fileEventType);
    }

    private List<String> fileNames;
    private Callback cbk;

    public FileChangeListener(@NotNull List<String> fileNames, Callback cbk) {
        this.fileNames = fileNames;
        this.cbk = cbk;
    }

    @Override
    public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        // Do nothing since no file changed
    }

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        handleFileChange(event, FileEventType.Change);
    }

    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        handleFileChange(event, FileEventType.Create);
    }

    @Override
    public void fileDeleted(@NotNull VirtualFileEvent event) {
        handleFileChange(event, FileEventType.Delete);
    }

    @Override
    public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        handleFileChange(event, FileEventType.Move);
    }

    @Override
    public void fileCopied(@NotNull VirtualFileCopyEvent event) {
        handleFileChange(event, FileEventType.Copy);
    }

    @Override
    public void beforePropertyChange(@NotNull VirtualFilePropertyEvent event) {
        // Do nothing since no file changed
    }

    @Override
    public void beforeContentsChange(@NotNull VirtualFileEvent event) {
        // Do nothing since no file changed
    }

    @Override
    public void beforeFileDeletion(@NotNull VirtualFileEvent event) {
        // Do nothing since no file changed
    }

    @Override
    public void beforeFileMovement(@NotNull VirtualFileMoveEvent event) {
        // Do nothing since no file changed
    }

    private void handleFileChange(@NotNull VirtualFileEvent event, FileEventType fileEventType) {
        if (event.isFromSave() || !fileEventType.equals(FileEventType.Change)) {
            if (fileNames.contains(event.getFileName())) {
                cbk.run(event, fileEventType);
            }
        }
    }
}