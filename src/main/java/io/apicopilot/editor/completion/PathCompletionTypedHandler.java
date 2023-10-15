package io.apicopilot.editor.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * 解决输入/不会触发自动完成问题.
 */
public class PathCompletionTypedHandler extends TypedHandlerDelegate {
    @NotNull
    @Override
    public Result checkAutoPopup(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (c == '/') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        }
        return Result.CONTINUE;
    }

}
