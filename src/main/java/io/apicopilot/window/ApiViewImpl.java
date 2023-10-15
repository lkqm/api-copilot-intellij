package io.apicopilot.window;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.treeStructure.Tree;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentRepository;
import io.apicopilot.document.LoadResult;
import io.apicopilot.document.topic.DocumentTopic;
import io.apicopilot.model.Request;
import io.apicopilot.util.UiUtils;
import io.apicopilot.window.support.PreviewState;
import io.apicopilot.window.tree.DocumentNode;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * API view impl.
 */
@State(
        name = "ApiView",
        storages = {@Storage("$PRODUCT_WORKSPACE_FILE$")}
)

public class ApiViewImpl implements PersistentStateComponent<Element>, ApiView {

    private final Project project;
    private boolean isInitialized;
    private ApiViewPanel panel;
    private ToolWindow toolWindow;


    public ApiViewImpl(Project project) {
        this.project = project;
    }

    @Override
    public synchronized void setup(ToolWindow toolWindow) {
        if (this.isInitialized) {
            return;
        }

        this.isInitialized = true;
        this.toolWindow = toolWindow;
        this.panel = new ApiViewPanel(project);

        // set tool window title actions
        AnAction action = ActionManager.getInstance().getAction("ApiViewToolbar");
        this.toolWindow.setTitleActions(Collections.singletonList(action));

        // set tool window content panel
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(panel, "", false);
        contentManager.addContent(content);

        // subscribe document topic
        project.getMessageBus().connect().subscribe(DocumentTopic.TOPIC, new DocumentTopic() {
            @Override
            public void onAdded(Document document) {
                UiUtils.run(() -> panel.onDocumentAdded(document));
            }

            @Override
            public void onModified(Document document) {
                UiUtils.run(() -> panel.onDocumentModified(document));
            }

            @Override
            public void onLoaded(Document document, LoadResult result) {
                UiUtils.run(() -> panel.onDocumentLoaded(document, result));
            }
        });
    }

    @Override
    public void select(String documentId, String path, String method) {
        if (!this.isInitialized) {
            ToolWindow window = ToolWindowManager.getInstance(this.project).getToolWindow(ApiViewToolWindowFactory.ID);
            this.setup(window);
        }
        toolWindow.show(() -> this.panel.select(documentId, method, path));
    }

    @Override
    public void refresh() {
        this.panel.refresh();
    }

    @Override
    public PreviewState switchPreviewState() {
        PreviewState state = this.panel.getPreviewState();
        PreviewState next = state.switchState();
        this.panel.setPreviewState(next);
        return next;
    }

    @Override
    public List<Document> getSelectedDocuments() {
        Tree tree = panel.getTreePane().getTree();
        DocumentNode[] documentNodes = tree.getSelectedNodes(DocumentNode.class, null);
        return Arrays.stream(documentNodes).map(node -> node.getData().getDocument()).collect(Collectors.toList());
    }

    @Override
    public void deleteDocuments(List<Document> documents) {
        List<String> documentIds = documents.stream().map(Document::getId).filter(Objects::nonNull).collect(Collectors.toList());
        DocumentRepository.getInstance(project).deletes(documentIds);
        documentIds.forEach(documentId -> panel.getTreePane().removeDocumentNode(documentId));
    }

    @Override
    public void locate() {
        PreviewState previewState = this.panel.getPreviewState();
        if (previewState == PreviewState.HIDDEN) {
            return;
        }
        ApiViewPreviewPane previewPane = this.panel.getPreviewPane();
        Document document = previewPane.getDocument();
        Request request = previewPane.getRequest();
        if (document != null && request != null) {
            this.select(document.getId(), request.getPath(), request.getMethod());
        }
    }

    @Override
    public ApiViewPanel getPanel() {
        return this.panel;
    }


    @Override
    public @Nullable Element getState() {
        return null;
    }

    @Override
    public void loadState(@NotNull Element element) {

    }
}
