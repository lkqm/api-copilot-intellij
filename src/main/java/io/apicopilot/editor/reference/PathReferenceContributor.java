package io.apicopilot.editor.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import io.apicopilot.util.PsiStringLiteralUtils;
import io.apicopilot.util.PsiStringLiteralUtils.StringLiteralType;
import org.jetbrains.annotations.NotNull;

/**
 * API path reference.
 */
public class PathReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(), new PathReferenceProvider());
    }

    public static class PathReferenceProvider extends PsiReferenceProvider {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            String text = element.getText();
            if (text == null) {
                return PsiReference.EMPTY_ARRAY;
            }

            StringLiteralType type = PsiStringLiteralUtils.getStringLiteralType(text);
            String path = (type == StringLiteralType.NONE) ? text : text.substring(type.getStartLength(), text.length() - type.getEndLength());
            if (path.length() > 1 && !path.startsWith("/")) {
                return PsiReference.EMPTY_ARRAY;
            }
            TextRange range = new TextRange(type.getStartLength(), text.length() - type.getEndLength());
            return new PsiReference[]{new PathReference(element, range, path)};
        }
    }
}