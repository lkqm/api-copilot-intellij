package io.apix.window.debug;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import io.apix.debug.AuthConfig;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Dialog for editing the document-level auth config.
 * Changes are persisted only when the user clicks OK.
 */
public class DocAuthDialog extends DialogWrapper {

    private final AuthPanel authPanel = new AuthPanel(false);
    private final Consumer<AuthConfig> onSave;

    public DocAuthDialog(Project project, AuthConfig current, Consumer<AuthConfig> onSave) {
        super(project);
        this.onSave = onSave;
        authPanel.setAuthConfig(current != null ? current : new AuthConfig());
        setTitle("Document Auth");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setPreferredSize(new Dimension(JBUI.scale(480), JBUI.scale(320)));
        wrapper.add(authPanel, BorderLayout.CENTER);
        return wrapper;
    }

    @Override
    protected void doOKAction() {
        onSave.accept(authPanel.getAuthConfig());
        super.doOKAction();
    }
}
