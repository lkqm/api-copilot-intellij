package io.apix.editor.completion;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;
import io.apix.util.PsiStringLiteralUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 解决字符串里面默认可能不会弹出代码提示.
 */
public class PathCompletionConfidence extends CompletionConfidence {

    @Override
    @NotNull
    public ThreeState shouldSkipAutopopup(@NotNull PsiElement contextElement, @NotNull PsiFile psiFile, int offset) {
        if (PsiStringLiteralUtils.isInStringLiteral(contextElement)) {
            String text = contextElement.getText();
            String value = PsiStringLiteralUtils.getStringLiteralValue(text);
            boolean valid = value != null && !value.isEmpty() && value.charAt(0) == '/';
            if (valid) {
                return ThreeState.NO;
            } else {
                return ThreeState.YES;
            }
        }
        return ThreeState.UNSURE;
    }

}
