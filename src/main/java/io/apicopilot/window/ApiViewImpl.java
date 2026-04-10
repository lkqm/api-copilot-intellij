package io.apicopilot.window;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.treeStructure.Tree;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentManager;
import io.apicopilot.document.DocumentRepository;
import io.apicopilot.document.LoadResult;
import io.apicopilot.document.topic.DocumentTopic;
import io.apicopilot.model.Request;
import io.apicopilot.util.UiUtils;
import io.apicopilot.window.ApiViewTabbedPane.SavedTabState;
import io.apicopilot.window.support.PreviewState;
import io.apicopilot.window.tree.DocumentNode;
import io.swagger.v3.oas.models.PathItem;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API view impl.
 */
@State(
        name = "ApiView",
        storages = {@Storage("$PRODUCT_WORKSPACE_FILE$")}
)

public class ApiViewImpl implements PersistentStateComponent<Element>, ApiView {

    private static final String ELEM_EXPANDED = "expanded";
    private static final String ELEM_KEY      = "key";
    private static final String ELEM_TABS     = "tabs";
    private static final String ELEM_TAB      = "tab";
    private static final String ATTR_DOC_ID   = "docId";
    private static final String ATTR_METHOD   = "method";
    private static final String ATTR_PATH     = "path";
    private static final String ATTR_SELECTED = "selected";
    private static final String ATTR_SPLITTER_PROPORTION = "splitterProportion";

    private final Project project;
    private boolean isInitialized;
    private ApiViewPanel panel;
    private ToolWindow toolWindow;
    /** Tree expansion keys loaded before the panel is ready; applied after firstLoad(). */
    private Set<String>        pendingExpandedKeys;
    /** Tab states loaded before the panel is ready; applied after firstLoad(). */
    private List<SavedTabState> pendingTabs;
    /** Splitter proportion loaded before the panel is ready; applied after panel creation. */
    private Float pendingSplitterProportion;


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
        if (pendingSplitterProportion != null) {
            panel.setSavedSplitterProportion(pendingSplitterProportion);
            pendingSplitterProportion = null;
        }

        // set tool window title actions
        AnAction action = ActionManager.getInstance().getAction("ApiViewToolbar");
        this.toolWindow.setTitleActions(Collections.singletonList(action));

        // set tool window content panel
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(panel, "", false);
        contentManager.addContent(content);

        // Restore tree expansion state and open tabs after firstLoad() completes.
        // firstLoad() is queued via smartInvokeLater in ApiViewPanel's constructor,
        // so scheduling another smartInvokeLater here guarantees it runs after.
        final Set<String>        keys = pendingExpandedKeys;
        final List<SavedTabState> tabs = pendingTabs;
        pendingExpandedKeys = null;
        pendingTabs         = null;
        if (keys != null || (tabs != null && !tabs.isEmpty())) {
            DumbService.getInstance(project).smartInvokeLater(() -> {
                if (keys != null) panel.getTreePane().applyExpandedKeys(keys);
                if (tabs != null) restoreTabs(tabs);
            });
        }

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

            @Override
            public void onUpdateDetected(Document document) {
                UiUtils.run(() -> panel.onDocumentUpdateDetected(document));
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
        Document document = this.panel.getTabPane().getCurrentDocument();
        Request request = this.panel.getTabPane().getCurrentRequest();
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
        Element root = new Element("ApiView");
        if (panel == null) return root;

        // Tree expansion
        Set<String> keys = panel.getTreePane().getExpandedKeys();
        if (!keys.isEmpty()) {
            Element expanded = new Element(ELEM_EXPANDED);
            keys.forEach(k -> expanded.addContent(new Element(ELEM_KEY).setText(k)));
            root.addContent(expanded);
        }

        // Open tabs
        List<SavedTabState> tabStates = panel.getTabPane().getTabStates();
        if (!tabStates.isEmpty()) {
            Element tabs = new Element(ELEM_TABS);
            for (SavedTabState s : tabStates) {
                Element tab = new Element(ELEM_TAB);
                tab.setAttribute(ATTR_DOC_ID,   s.docId);
                tab.setAttribute(ATTR_METHOD,    s.method);
                tab.setAttribute(ATTR_PATH,      s.path);
                tab.setAttribute(ATTR_SELECTED,  String.valueOf(s.selected));
                tabs.addContent(tab);
            }
            root.addContent(tabs);
        }

        root.setAttribute(ATTR_SPLITTER_PROPORTION, String.valueOf(panel.getSavedSplitterProportion()));
        return root;
    }

    @Override
    public void loadState(@NotNull Element element) {
        String splitterProportionAttr = element.getAttributeValue(ATTR_SPLITTER_PROPORTION);
        if (splitterProportionAttr != null) {
            try {
                float proportion = Float.parseFloat(splitterProportionAttr);
                if (panel != null) panel.setSavedSplitterProportion(proportion);
                else pendingSplitterProportion = proportion;
            } catch (NumberFormatException ignored) {
                pendingSplitterProportion = null;
            }
        }

        // Tree expansion
        Element expanded = element.getChild(ELEM_EXPANDED);
        if (expanded != null) {
            Set<String> keys = new HashSet<>();
            for (Element child : expanded.getChildren(ELEM_KEY)) {
                String text = child.getTextTrim();
                if (!text.isEmpty()) keys.add(text);
            }
            if (!keys.isEmpty()) {
                if (panel != null) panel.getTreePane().applyExpandedKeys(keys);
                else pendingExpandedKeys = keys;
            }
        }

        // Open tabs
        Element tabsElem = element.getChild(ELEM_TABS);
        if (tabsElem != null) {
            List<SavedTabState> tabs = new ArrayList<>();
            for (Element tab : tabsElem.getChildren(ELEM_TAB)) {
                String docId    = tab.getAttributeValue(ATTR_DOC_ID);
                String method   = tab.getAttributeValue(ATTR_METHOD);
                String path     = tab.getAttributeValue(ATTR_PATH);
                boolean selected = "true".equals(tab.getAttributeValue(ATTR_SELECTED));
                if (docId != null && method != null && path != null) {
                    tabs.add(new SavedTabState(docId, method, path, selected));
                }
            }
            if (!tabs.isEmpty()) {
                if (panel != null) restoreTabs(tabs);
                else pendingTabs = tabs;
            }
        }
    }

    private void restoreTabs(List<SavedTabState> tabs) {
        List<Document> documents = DocumentManager.getInstance(project).getDocuments();
        Map<String, Document> docById = documents.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(Document::getId, d -> d, (a, b) -> a));

        // Process non-selected tabs first, selected tab last (so it ends up focused)
        List<SavedTabState> sorted = new ArrayList<>(tabs);
        sorted.sort((a, b) -> Boolean.compare(a.selected, b.selected));

        Document  selDoc = null;
        Request   selReq = null;

        for (SavedTabState s : sorted) {
            Document doc = docById.get(s.docId);
            if (doc == null || doc.getOpenApi() == null || doc.getOpenApi().getPaths() == null) continue;
            PathItem pathItem = doc.getOpenApi().getPaths().get(s.path);
            if (pathItem == null) continue;
            try {
                PathItem.HttpMethod httpMethod = PathItem.HttpMethod.valueOf(s.method.toUpperCase());
                io.swagger.v3.oas.models.Operation op = pathItem.readOperationsMap().get(httpMethod);
                if (op == null) continue;
                Request request = new Request(s.path, s.method, op);
                if (panel.getPreviewState() == PreviewState.HIDDEN) {
                    panel.setPreviewState(PreviewState.VERTICAL);
                }
                panel.getTabPane().openOrFocus(doc, request);
                if (s.selected) { selDoc = doc; selReq = request; }
            } catch (IllegalArgumentException ignored) { /* unknown HTTP method */ }
        }

        // Re-select the previously active tab
        if (selDoc != null) panel.getTabPane().openOrFocus(selDoc, selReq);
    }
}
