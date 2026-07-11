package io.apix.window.dialog;

import com.intellij.openapi.ui.ValidationInfo;
import io.apix.document.Document;

import javax.swing.*;

public interface DocumentEditForm {

    JPanel getPanel();

    void set(Document document);

    Document get();

    ValidationInfo validate();

}
