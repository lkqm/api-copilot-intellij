package apicopilot.window;

import apicopilot.apidoc.DocumentProvider;
import apicopilot.apidoc.FileDocumentProvider;
import apicopilot.model.Document;
import apicopilot.window.toolbar.ApiToolWindowToolbar;
import apicopilot.window.tree.ApiTree;
import apicopilot.window.tree.ApiTreeTopic;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @see #refreshTree()
 */
public class ApiToolWindow extends JPanel {

    private final Project project;
    private final ApiTree apiTree;

    public ApiToolWindow(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.apiTree = new ApiTree(project);
        ActionToolbar actionToolbar = ApiToolWindowToolbar.getToolbar();
        actionToolbar.setTargetComponent(this);
        this.add(actionToolbar.getComponent(), BorderLayout.NORTH);
        this.add(apiTree, BorderLayout.CENTER);

        project.getMessageBus().connect().subscribe(ApiTreeTopic.TOPIC, apiTree::renderTree);
        DumbService.getInstance(project).smartInvokeLater(this::firstLoad);
    }

    private void firstLoad() {
        refreshTree();
    }

    /**
     * refresh the tree.
     */
    public void refreshTree() {
        ApiTreeTopic serviceTreeTopic = project.getMessageBus().syncPublisher(ApiTreeTopic.TOPIC);
        DumbService.getInstance(project).runWhenSmart(() -> serviceTreeTopic.action(getApiTreeData()));
    }

    /**
     * navigate to the selected api
     */
    public boolean navigateToApi(String connectionId, String method, String path) {
        return apiTree.navigateToApi(connectionId, method, path);
    }

    @NotNull
    private ApiTree.RenderArgs getApiTreeData() {
        DocumentProvider documentProvider = new FileDocumentProvider(project);
        Document document = documentProvider.getDocument();
        ApiTree.RenderArgs data = new ApiTree.RenderArgs();
        data.setDocument(document);
        return data;
    }
}
