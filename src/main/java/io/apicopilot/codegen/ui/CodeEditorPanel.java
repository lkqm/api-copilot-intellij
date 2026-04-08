package io.apicopilot.codegen.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;

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

        // Use LightVirtualFile so the document is backed by a VirtualFile.
        // This allows IntelliJ's PSI infrastructure to associate a PsiFile with the
        // document, which is required for language services such as code folding,
        // syntax highlighting, and the DaemonCodeAnalyzer to function properly.
        // Always writable so programmatic setText() works.
        // The editor's own readonly flag (passed to createEditor) handles user-edit prevention.
        LightVirtualFile vf = new LightVirtualFile("_body_." + extension, fileType, code);
        Document document = ApplicationManager.getApplication()
                .runReadAction((com.intellij.openapi.util.Computable<Document>) () ->
                        FileDocumentManager.getInstance().getDocument(vf));
        if (document == null) {
            // Fallback: orphan document (no folding, but editor still works)
            document = EditorFactory.getInstance().createDocument(code);
        }

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
