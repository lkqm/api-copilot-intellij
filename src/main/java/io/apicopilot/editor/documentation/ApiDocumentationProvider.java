package io.apicopilot.editor.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import io.apicopilot.editor.ApiPsiElement;
import io.apicopilot.model.Api;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * API路径元素的文档提供.
 */
public class ApiDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public @Nullable @Nls String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (element instanceof ApiPsiElement) {
            Api api = ((ApiPsiElement) element).getValue();
            return api.getRequest().getMethod() + " " + api.getRequest().getPath() + " " + StringUtils.defaultString(api.getRequest().getOperation().getSummary());
        }
        return null;
    }

    @Override
    public @Nullable @Nls String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        return super.generateDoc(element, originalElement);
    }
}
