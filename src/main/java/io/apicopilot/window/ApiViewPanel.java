package io.apicopilot.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.concurrency.AppExecutorUtil;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentManager;
import io.apicopilot.document.LoadResult;
import io.apicopilot.document.SyncStatus;
import io.apicopilot.model.Request;
import io.apicopilot.util.NotificationUtils;
import io.apicopilot.window.support.PreviewState;
import io.apicopilot.window.tree.RequestNode;
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

    private static final int LEFT_PANEL_MIN_WIDTH = 260;

    private final Project project;
    @Getter
    private final ApiViewTreePane treePane;
    @Getter
    private final ApiViewTabbedPane tabPane;
    private final JBSplitter splitter;
    @Getter
    private PreviewState previewState;

    public ApiViewPanel(@NotNull Project project) {
        super(true, true);
        this.project = project;
        this.tabPane = new ApiViewTabbedPane(project);
        this.previewState = PreviewState.HIDDEN;
        this.treePane = new ApiViewTreePane(project);
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMinimumSize(new Dimension(LEFT_PANEL_MIN_WIDTH, 0));
        leftPanel.setBorder(new SideBorder(JBColor.border(), SideBorder.RIGHT));
        leftPanel.add(createToolbar(), BorderLayout.NORTH);
        leftPanel.add(treePane, BorderLayout.CENTER);
        this.splitter = new JBSplitter(false, "ApiViewPanelSplitter", 1);
        this.splitter.setDividerWidth(3);
        this.splitter.setFirstComponent(leftPanel);
        this.setContent(this.splitter);
        initEvent();
        DumbService.getInstance(project).smartInvokeLater(this::firstLoad);
    }

    private void initEvent() {
        Tree tree = this.treePane.getTree();

        // Single click: open/replace preview tab (or focus if already a permanent tab)
        tree.addTreeSelectionListener(e -> {
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
        ensureRightPaneVisible();
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
            if (splitter.getProportion() >= 0.99f) {
                splitter.setProportion(0.4f);
            }
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
        return treePane.select(connectionId, method, path);
    }

    /**
     * Toggle the right pane visibility (toolbar button).
     */
    public void setPreviewState(PreviewState type) {
        this.previewState = type;
        if (type == PreviewState.HIDDEN) {
            splitter.setSecondComponent(null);
            splitter.setProportion(1f);
        } else {
            splitter.setSecondComponent(tabPane);
            if (splitter.getProportion() >= 0.99f) {
                splitter.setProportion(0.4f);
            }
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
}
