package io.apicopilot.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.window.debug.ApiDebugPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Detail pane for a single API endpoint: Docs / Debug tabs.
 */
public class ApiViewDetailPane extends JPanel implements Disposable {

    private final ApiViewPreviewPane previewPane;
    private final ApiDebugPanel      debugPanel;
    private final JBTabbedPane       tabs;

    public ApiViewDetailPane(Project project) {
        super(new BorderLayout());

        tabs = new JBTabbedPane(JBTabbedPane.TOP);

        previewPane = new ApiViewPreviewPane(project, () -> tabs.setSelectedIndex(1));
        debugPanel  = new ApiDebugPanel(project);

        tabs.addTab("Docs",  previewPane);
        tabs.addTab("Debug", debugPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void setRequest(Document document, Request request) {
        previewPane.setRequest(document, request);
        debugPanel.setRequest(document, request);
    }

    public void refreshDebugUrl() {
        debugPanel.refreshUrlField();
    }

    public Document getDocument() { return previewPane.getDocument(); }
    public Request  getRequest()  { return previewPane.getRequest(); }

    @Override
    public void dispose() {
        previewPane.dispose();
        debugPanel.dispose();
    }
}
