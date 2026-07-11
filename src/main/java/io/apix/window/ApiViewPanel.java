package io.apix.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.concurrency.AppExecutorUtil;
import io.apix.document.Document;
import io.apix.document.DocumentManager;
import io.apix.document.LoadResult;
import io.apix.document.SyncStatus;
import io.apix.model.Request;
import io.apix.util.NotificationUtils;
import io.apix.window.support.PreviewState;
import io.apix.window.tree.RequestNode;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * API view content panel.
 *
 * @see #refresh()
 * @see #select(String, String, String)
 */
public class ApiViewPanel extends SimpleToolWindowPanel implements Disposable {

    private static final float DEFAULT_SPLITTER_PROPORTION = 0.4f;

    private final Project project;
    @Getter
    private final ApiViewTreePane treePane;
    @Getter
    private final ApiViewTabbedPane tabPane;
    private final JBSplitter splitter;
    @Getter
    private PreviewState previewState;
    private float savedSplitterProportion = DEFAULT_SPLITTER_PROPORTION;

    public ApiViewPanel(@NotNull Project project) {
        super(true, true);
        this.project = project;
        this.tabPane = new ApiViewTabbedPane(project, () -> setPreviewState(PreviewState.HIDDEN));
        this.previewState = PreviewState.HIDDEN;
        this.treePane = new ApiViewTreePane(project);
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMinimumSize(new Dimension(0, 0));
        leftPanel.add(createToolbar(), BorderLayout.NORTH);
        leftPanel.add(treePane, BorderLayout.CENTER);
        this.splitter = new JBSplitter(false, "ApiViewPanelSplitter", 1);
        this.splitter.setHonorComponentsMinimumSize(false);
        this.splitter.setFirstComponent(leftPanel);
        this.splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION, event -> {
            if (previewState != PreviewState.HIDDEN && splitter.getSecondComponent() != null) {
                savedSplitterProportion = splitter.getProportion();
            }
        });
        this.setContent(this.splitter);
        initEvent();
        DumbService.getInstance(project).smartInvokeLater(this::firstLoad);
    }

    private void initEvent() {
        Tree tree = this.treePane.getTree();

        // Single click: open/replace preview tab (or focus if already a permanent tab)
        tree.addTreeSelectionListener(e -> {
            if (Boolean.TRUE.equals(tree.getClientProperty(ApiViewTreePane.SUPPRESS_SELECTION_PREVIEW))) {
                return;
            }
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (treeNode instanceof RequestNode) {
                RequestNode.Context data = ((RequestNode) treeNode).getData();
                openPreview(data.getDocument(), data.getRequest());
            }
        });

        // Enter key: open or focus a tab for the selected API
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (treeNode instanceof RequestNode) {
                        RequestNode.Context data = ((RequestNode) treeNode).getData();
                        openTab(data.getDocument(), data.getRequest());
                    }
                }
            }
        });

        // Double click: open or focus a tab for the selected API
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (treeNode instanceof RequestNode) {
                        RequestNode.Context data = ((RequestNode) treeNode).getData();
                        openTab(data.getDocument(), data.getRequest());
                    }
                }
            }
        });
    }

    /** Single-click: open preview tab (shows right pane if hidden). */
    private void openPreview(Document document, Request request) {
        if (previewState == PreviewState.HIDDEN) {
            return;
        }
        tabPane.previewOrFocus(document, request);
    }

    /** Double-click: open permanent tab (promotes preview if applicable). */
    private void openTab(Document document, Request request) {
        ensureRightPaneVisible();
        tabPane.openOrFocus(document, request);
    }

    private void ensureRightPaneVisible() {
        if (previewState == PreviewState.HIDDEN) {
            previewState = PreviewState.VERTICAL;
            splitter.setSecondComponent(tabPane);
            splitter.setProportion(savedSplitterProportion);
        }
    }

    @NotNull
    private JComponent createToolbar() {
        AnAction action = ActionManager.getInstance().getAction("ApiViewToolWindowToolbar");
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
                ActionPlaces.TOOLBAR,
                action instanceof ActionGroup ? ((ActionGroup) action) : new DefaultActionGroup(),
                true
        );
        actionToolbar.setTargetComponent(this);
        return actionToolbar.getComponent();
    }

    private void firstLoad() {
        List<Document> documents = DocumentManager.getInstance(project).getDocuments();
        for (Document document : documents) {
            treePane.addDocumentNode(document);
        }
    }

    public void refresh() {
        DocumentManager manager = DocumentManager.getInstance(project);
        List<Document> documents = manager.getDocuments();
        if (documents.isEmpty()) {
            return;
        }

        DumbService.getInstance(project).runWhenSmart(() -> {
            documents.forEach(document -> {
                document.setSyncStatus(SyncStatus.SYNCING);
                treePane.refreshDocumentNode(document, false);
            });
            List<CompletableFuture<LoadResult>> futures = documents.stream()
                    .map(document -> CompletableFuture.supplyAsync(
                            () -> manager.reloadDocument(document),
                            AppExecutorUtil.getAppExecutorService()))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        List<String> failures = new ArrayList<>();
                        for (int i = 0; i < documents.size(); i++) {
                            LoadResult result = futures.get(i).join();
                            if (!result.isSuccess()) {
                                failures.add(documents.get(i).getName() + ": " + result.getFailReason());
                            }
                        }
                        if (!failures.isEmpty()) {
                            NotificationUtils.notifyError("Refresh document failed", String.join("\n", failures));
                        }
                    });
        });
    }

    public boolean select(String connectionId, String method, String path) {
        boolean selected = treePane.select(connectionId, method, path);
        if (selected) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePane.getTree().getLastSelectedPathComponent();
            if (treeNode instanceof RequestNode) {
                RequestNode.Context data = ((RequestNode) treeNode).getData();
                ensureRightPaneVisible();
                openPreview(data.getDocument(), data.getRequest());
            }
        }
        return selected;
    }

    /**
     * Toggle the right pane visibility (toolbar button).
     */
    public void setPreviewState(PreviewState type) {
        this.previewState = type;
        if (type == PreviewState.HIDDEN) {
            if (splitter.getSecondComponent() != null) {
                savedSplitterProportion = splitter.getProportion();
            }
            splitter.setSecondComponent(null);
        } else {
            splitter.setSecondComponent(tabPane);
            splitter.setProportion(savedSplitterProportion);
        }
    }

    @Override
    public void dispose() {
        tabPane.dispose();
    }

    @Override
    @Nullable
    public Object getData(@NotNull @NonNls String dataId) {
        if (CommonDataKeys.PROJECT.is(dataId)) {
            return project;
        } else if (PlatformDataKeys.TREE_EXPANDER.is(dataId)) {
            return this.treePane.getTreeExpander();
        }
        return null;
    }

    public void onDocumentAdded(Document document) {
        treePane.addDocumentNode(document);
    }

    public void onDocumentLoaded(Document document, LoadResult result) {
        treePane.refreshDocumentNode(document, result.isSuccess() && result.isChanged());
    }

    public void onDocumentModified(Document document) {
        treePane.refreshDocumentNode(document, false);
    }

    public void onDocumentUpdateDetected(Document document) {
        treePane.refreshDocumentNode(document, false);
    }

    public float getSavedSplitterProportion() {
        return savedSplitterProportion;
    }

    public void setSavedSplitterProportion(float proportion) {
        savedSplitterProportion = Float.isNaN(proportion) || Float.isInfinite(proportion)
                ? DEFAULT_SPLITTER_PROPORTION
                : proportion;
        if (previewState != PreviewState.HIDDEN && splitter.getSecondComponent() != null) {
            splitter.setProportion(savedSplitterProportion);
        }
    }
}
