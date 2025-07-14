package io.apicopilot.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;

public class FileWriteUtils {
    /**
     * 写入文本到指定路径（支持相对路径、绝对路径）
     *
     * @param project  当前 Project
     * @param filePath 可以是绝对路径或项目相对路径
     * @param text     要写入的内容
     */
    public static void write(Project project, String filePath, String text) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                String basePath = project.getBasePath();
                if (basePath == null) {
                    throw new IllegalStateException("Unable to determine base path");
                }

                File ioFile = new File(filePath);
                if (!ioFile.isAbsolute()) {
                    ioFile = new File(basePath, filePath);
                }

                File parentDir = ioFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        throw new IOException("Unable to create directory: " + parentDir.getAbsolutePath());
                    }
                }

                if (!ioFile.exists()) {
                    if (!ioFile.createNewFile()) {
                        throw new IOException("Unable to create file: " + ioFile.getAbsolutePath());
                    }
                }

                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile);
                if (virtualFile != null) {
                    VfsUtil.saveText(virtualFile, text);
                } else {
                    throw new IOException("Unable to save file: " + ioFile.getAbsolutePath());
                }

            } catch (IOException e) {
                throw new RuntimeException("Saving file failed: " + filePath, e);
            }
        });
    }
}
