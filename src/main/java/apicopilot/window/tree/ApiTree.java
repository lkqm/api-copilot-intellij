package apicopilot.window.tree;


import apicopilot.icon.HttpMethodIcons;
import apicopilot.model.Document;
import apicopilot.model.Request;
import apicopilot.util.OpenApiUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.Tree;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;


/**
 * API树
 */
public class ApiTree extends JBScrollPane {

    private final Project project;

    private final Tree tree;
    private final Map<String, RequestNode> apiNodes = new HashMap<>();


    public ApiTree(@NotNull Project project) {
        this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.project = project;
        this.tree = new SimpleTree();
        ((DefaultTreeModel) this.tree.getModel()).setRoot(new DefaultMutableTreeNode());
        this.tree.setCellRenderer(new ApiTreeCellRenderer());
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        this.setViewportView(this.tree);
        initEvent();
    }


    /**
     * 导航到特定API节点
     */
    public boolean navigateToApi(String connectionId, String method, String path) {
        String apiId = getApiNavigationId(connectionId, method, path);
        RequestNode node = apiNodes.get(apiId);
        if (node == null) {
            return false;
        }
        // 有节点到根路径数组
        TreeNode[] nodes = ((DefaultTreeModel) tree.getModel()).getPathToRoot(node);
        TreePath treePath = new TreePath(nodes);
        tree.setSelectionPath(treePath);
        return true;
    }

    private String getApiNavigationId(String connectionId, String method, String path) {
        return String.format("%s:%s:%s", connectionId, path, method);
    }

    /**
     * 渲染树
     */
    public void renderTree(RenderArgs args) {
        Document document = args.getDocument();
        if (document == null) {
            return;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DocumentNode documentNode = buildDocumentNode(document);
        root.add(documentNode);
        ((DefaultTreeModel) tree.getModel()).setRoot(root);
    }

    private DocumentNode buildDocumentNode(Document document) {
        String id = document.getId();
        String name = document.getName();
        OpenAPI openApi = document.getOpenApi();
        DocumentNode.Data connectionData = DocumentNode.Data.builder()
                .id(id)
                .name(name)
                .apiCounts(OpenApiUtils.countApi(openApi))
                .build();
        DocumentNode documentNode = new DocumentNode(AllIcons.Nodes.DataTables, connectionData);
        Map<String, ApiTreeNode<?>> folderNodes = new HashMap<>();

        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                Request request = new Request();
                request.setMethod(method.name());
                request.setPath(path);
                request.setOperation(operation);

                RequestNode.Data data = RequestNode.Data.builder()
                        .request(request)
                        .build();
                RequestNode node = new RequestNode(HttpMethodIcons.getHttpMethodIcon(method.name()), data);
                if (operation.getTags() != null && !operation.getTags().isEmpty()) {
                    String tag = operation.getTags().get(0);
                    FolderNode.Data folderData = FolderNode.Data.builder().tag(tag).build();
                    ApiTreeNode<?> folderNode = folderNodes.computeIfAbsent(tag, key -> new FolderNode(AllIcons.Nodes.Folder, folderData));
                    folderNode.add(node);
                } else {
                    documentNode.add(node);
                }
                // put in navigation map
                String apiId = getApiNavigationId(id, method.name(), path);
                apiNodes.put(apiId, node);
            });
        });
        folderNodes.values().forEach(documentNode::add);
        return documentNode;
    }

    private void initEvent() {
        // 节点选中事件
        tree.addTreeSelectionListener(e -> {
        });

        // 鼠标点击事件
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                /*
                boolean click = SwingUtilities.isLeftMouseButton(e);
                int clickCount = e.getClickCount();
                 */
            }

            @Override
            public void mousePressed(MouseEvent e) {
                boolean isRightClick = SwingUtilities.isRightMouseButton(e);
                if (!isRightClick) {
                    return;
                }

                JPopupMenu popupMenu = null;
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (treeNode == null) {
                    return;
                }
                if (treeNode instanceof RequestNode) {
                    popupMenu = ((RequestNode) treeNode).getPopupMenu();
                }
                showPopupMenu(e, popupMenu);
            }

            private void showPopupMenu(@NotNull MouseEvent event, @Nullable JPopupMenu menu) {
                if (menu == null) {
                    return;
                }
                menu.show(tree, event.getX(), event.getY());
            }
        });

        // 键盘事件
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // TODO: 回车打开对于API文档详情
                }
            }
        });
    }

    /**
     * API树渲染参数
     */
    @Data
    public static class RenderArgs {

        private Document document;

    }
}
