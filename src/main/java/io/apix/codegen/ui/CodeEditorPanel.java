package io.apix.codegen.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public class CodeEditorPanel extends JPanel implements Disposable {

    private final Project project;
    private Editor editor;
    private String extension = "text";
    private boolean readonly;

    public CodeEditorPanel(Project project) {
        this(project, true);
    }

    public CodeEditorPanel(Project project, boolean readonly) {
        super(new BorderLayout());
        this.project = project;
        this.readonly = readonly;
        createEditor("", extension);
    }

    private void createEditor(String code, String extension) {
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension);

        // Use a plain document plus a file-type highlighter so we keep syntax coloring
        // without creating a PSI/VirtualFile identity that would trigger project-level analysis.
        Document document = EditorFactory.getInstance().createDocument(code);

        editor = EditorFactory.getInstance().createEditor(document, project, fileType, this.readonly);

        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setRightMarginShown(false);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);
        settings.setCaretRowShown(false);
        if (editor instanceof EditorEx) {
            ((EditorEx) editor).setHighlighter(
                    EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));
            JScrollBar scrollBar = ((EditorEx) editor).getScrollPane().getVerticalScrollBar();
            scrollBar.setOpaque(false);
        }

        add(editor.getComponent(), BorderLayout.CENTER);
    }

    /**
     * 修改内容
     */
    public void setText(String text, String extension) {
        if (extension.equalsIgnoreCase(this.extension)) {
            Document document = editor.getDocument();
            WriteCommandAction.runWriteCommandAction(project, () -> document.setText(text));
            return;
        }

        // rebuild editor for new file type
        if (editor != null) {
            remove(editor.getComponent());
            EditorFactory.getInstance().releaseEditor(editor);
        }
        createEditor(text, extension);
        this.extension = extension;
        revalidate();
        repaint();
    }

    /**
     * 获取内容
     */
    public String getText() {
        return editor != null ? editor.getDocument().getText() : "";
    }

    /**
     * 释放编辑器资源，防止内存泄漏
     */
    @Override
    public void dispose() {
        if (editor != null) {
            EditorFactory.getInstance().releaseEditor(editor);
            editor = null;
        }
    }
}
