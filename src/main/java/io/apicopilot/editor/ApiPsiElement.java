package io.apicopilot.editor;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import io.apicopilot.editor.reference.PathReference;
import io.apicopilot.icon.HttpMethodIcons;
import io.apicopilot.model.Api;
import io.apicopilot.window.ApiView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Path psi element.
 */
@AllArgsConstructor
public final class ApiPsiElement extends FakePsiElement implements SyntheticElement {

    private final PathReference reference;
    @Getter
    private final Api value;

    @Override
    public PsiElement getParent() {
        return reference.getElement();
    }


    @Override
    public void navigate(boolean requestFocus) {
        ApiView apiView = ApiView.getInstance(getProject());
        apiView.select(value.getDocument().getId(), value.getRequest().getPath(), value.getRequest().getMethod());
    }

    @Override
    public String getPresentableText() {
        return value.getRequest().getPath() + " " + value.getRequest().getOperation().getSummary();
    }

    @Override
    public String getName() {
        return value.getRequest().getPath() + " " + value.getRequest().getOperation().getSummary();
    }

    @Override
    public TextRange getTextRange() {
        TextRange rangeInElement = reference.getRangeInElement();
        TextRange elementRange = reference.getElement().getTextRange();
        return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
    }

    @Override
    public @Nullable Icon getIcon(boolean open) {
        return HttpMethodIcons.getHttpMethodIcon(value.getRequest().getMethod());
    }


}