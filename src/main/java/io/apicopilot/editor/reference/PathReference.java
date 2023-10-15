package io.apicopilot.editor.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import io.apicopilot.document.DocumentManager;
import io.apicopilot.editor.ApiPsiElement;
import io.apicopilot.model.Api;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * API path reference.
 */
public class PathReference extends PsiPolyVariantReferenceBase<PsiElement> {

    @Getter
    private final String path;

    public PathReference(@NotNull PsiElement element, TextRange range, String path) {
        super(element, range, true);
        this.path = path;
    }

    @Override
    public boolean isSoft() {
        return true;
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean b) {
        DocumentManager documentManager = DocumentManager.getInstance(getElement().getProject());
        List<Api> apis = documentManager.getApi(path, null);
        return apis.stream().map(api -> {
            ApiPsiElement element = new ApiPsiElement(this, api);
            return new PsiElementResolveResult(element);
        }).toArray(ResolveResult[]::new);
    }


}
