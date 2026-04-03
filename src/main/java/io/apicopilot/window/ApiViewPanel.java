package io.apicopilot.window;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBSplitter;
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
public class ApiViewPanel extends SimpleToolWindowPanel {

    private final Project project;
    @Getter
    private final ApiViewTreePane treePane;
    @Getter
    private final ApiViewPreviewPane previewPane;
    private final JBSplitter splitter;
    @Getter
    private PreviewState previewState;

    public ApiViewPanel(@NotNull Project project) {
        super(true, true);
        this.setToolbar(createToolbar());
        this.project = project;
        this.previewPane = new ApiViewPreviewPane();
        this.previewState = PreviewState.HIDDEN;
        this.treePane = new ApiViewTreePane(project);
        this.splitter = new JBSplitter(false, "ApiViewPanelSplitter", 1);
        this.splitter.setFirstComponent(treePane);
        this.add(this.splitter);
        initEvent();
        DumbService.getInstance(project).smartInvokeLater(this::firstLoad);
    }

    private void initEvent() {
        Tree tree = this.treePane.getTree();
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (treeNode instanceof RequestNode) {
                RequestNode.Context data = ((RequestNode) treeNode).getData();
                showRequestDetail(data.getDocument(), data.getRequest());
            }
        });

        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (ApiViewPanel.this.previewState != PreviewState.HIDDEN) {
                        return;
                    }
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (treeNode instanceof RequestNode) {
                        setPreviewState(PreviewState.VERTICAL);
                    }
                }
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (ApiViewPanel.this.previewState != PreviewState.HIDDEN) {
                        return;
                    }
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (treeNode instanceof RequestNode) {
                        setPreviewState(PreviewState.VERTICAL);
                    }
                }
            }
        });


    }

    private void showRequestDetail(Document document, Request request) {
        JBSplitter splitter = ApiViewPanel.this.splitter;
        PreviewState state = this.previewState;
        if (state == PreviewState.HIDDEN) {
            splitter.setSecondComponent(null);
        } else if (state == PreviewState.VERTICAL) {
            splitter.setSecondComponent(previewPane);
            if (splitter.getProportion() == 1) {
                splitter.setProportion(0.4f);
            }
        }

        if (state != PreviewState.HIDDEN && request != null) {
            previewPane.setRequest(document, request);
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

    /**
     * 刷新面板.
     */
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

    /**
     * 选中指定请求.
     */
    public boolean select(String connectionId, String method, String path) {
        return treePane.select(connectionId, method, path);
    }

    /**
     * 控制详情面板
     */
    public void setPreviewState(PreviewState type) {
        // show current selected api
        Document document = null;
        Request api = null;
        if (type != PreviewState.HIDDEN) {
            RequestNode[] nodes = treePane.getTree().getSelectedNodes(RequestNode.class, null);
            if (nodes.length > 0) {
                RequestNode.Context data = nodes[0].getData();
                api = data.getRequest();
                document = data.getDocument();
            }
        }

        this.previewState = type;
        showRequestDetail(document, api);
    }

    @NotNull
    private ApiViewTreePane.RenderArgs getApiTreeData() {
        DocumentManager manager = DocumentManager.getInstance(project);
        List<Document> documents = manager.getDocuments();

        ApiViewTreePane.RenderArgs data = new ApiViewTreePane.RenderArgs();
        data.setDocuments(documents);
        return data;
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


    /**
     * 文档添加后
     */
    public void onDocumentAdded(Document document) {
        treePane.addDocumentNode(document);
    }

    /**
     * 文档加载后处理
     */
    public void onDocumentLoaded(Document document, LoadResult result) {
        treePane.refreshDocumentNode(document, result.isSuccess() && result.isChanged());
    }

    /**
     * 文档配置修改后
     */
    public void onDocumentModified(Document document) {
        treePane.refreshDocumentNode(document, false);
    }
}
