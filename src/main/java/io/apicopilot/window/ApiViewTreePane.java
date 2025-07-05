package io.apicopilot.window;


import com.intellij.icons.AllIcons;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.Tree;
import io.apicopilot.document.Document;
import io.apicopilot.icon.HttpMethodIcons;
import io.apicopilot.model.Request;
import io.apicopilot.util.OpenApiUtils;
import io.apicopilot.window.tree.*;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * API view tree panel.
 *
 * @see #render(RenderArgs)
 * @see #addDocumentNode(Document)
 * @see #removeDocumentNode(String)
 * @see #setDocumentNodeDisable(String)
 */
@Slf4j
public class ApiViewTreePane extends JBScrollPane {

    private final Project project;
    @Getter
    private final Tree tree;
    @Getter
    private final DefaultTreeModel treeModel;
    private TreeExpander treeExpander;
    private final Map<String, RequestNode> requestNodes = new HashMap<>();
    private final Map<String, DocumentNode> documentNodes = new HashMap<>();


    public ApiViewTreePane(@NotNull Project project) {
        this.project = project;
        this.tree = createTree();
        this.treeModel = (DefaultTreeModel) tree.getModel();
        this.setViewportView(this.tree);
        initEvent();
    }

    private Tree createTree() {
        Tree tree = new SimpleTree();
        ((DefaultTreeModel) tree.getModel()).setRoot(new DefaultMutableTreeNode());
        tree.setCellRenderer(new ApiViewCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        return tree;
    }

    /**
     * 渲染树
     */
    public void render(RenderArgs args) {
        List<Document> documents = args.getDocuments();
        if (documents == null || documents.isEmpty()) {
            return;
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        documents.forEach(document -> root.add(buildDocumentNode(document, null)));
        ((DefaultTreeModel) tree.getModel()).setRoot(root);
    }

    /**
     * 选中节点
     */
    public boolean select(String documentId, String method, String path) {
        String nodeId = getRequestNodeId(documentId, method, path);
        RequestNode node = requestNodes.get(nodeId);
        if (node == null) {
            return false;
        }
        // 有节点到根路径数组
        TreeNode[] nodes = treeModel.getPathToRoot(node);
        TreePath treePath = new TreePath(nodes);
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
        return true;
    }

    private String getRequestNodeId(String documentId, String method, String path) {
        return String.format("%s:%s:%s", documentId, path, method);
    }

    /**
     * 获取树折叠器.
     */
    public TreeExpander getTreeExpander() {
        TreeExpander expander = this.treeExpander;
        if (expander == null) {
            expander = new DefaultTreeExpander(this.tree);
            this.treeExpander = expander;
        }
        return expander;
    }

    /**
     * 删除文档节点
     */
    public void removeDocumentNode(String documentId) {
        DocumentNode node = documentNodes.get(documentId);
        if (node != null) {
            treeModel.removeNodeFromParent(node);
            documentNodes.remove(documentId);
        }
    }

    /**
     * 增加文档节点
     */
    public void addDocumentNode(Document document) {
        DocumentNode node = documentNodes.get(document.getId());
        if (node == null) {
            node = buildDocumentNode(document, null);
            MutableTreeNode root = (MutableTreeNode) treeModel.getRoot();
            treeModel.insertNodeInto(node, root, root.getChildCount());
            treeModel.nodeStructureChanged(root);
        }
    }

    /**
     * 刷新文档节点
     *
     * @param document        文档
     * @param includeChildren 是否刷新子节点
     */
    public void refreshDocumentNode(Document document, boolean includeChildren) {
        DocumentNode node = documentNodes.get(document.getId());
        if (node == null) {
            return;
        }

        if (includeChildren) {
            node.removeAllChildren();
            buildDocumentNode(document, node);
            this.getTreeModel().nodeChanged(node);
            this.getTreeModel().nodeStructureChanged(node);
        } else {
            node.getData().setDocument(document);
            this.getTreeModel().nodeChanged(node);
        }
    }


    public void setDocumentNodeDisable(String documentId) {
        DocumentNode node = documentNodes.get(documentId);
        if (node != null) {
            treeModel.reload(node);
        }
    }

    private DocumentNode buildDocumentNode(Document document, @Nullable DocumentNode documentNode) {
        String id = document.getId();
        String name = document.getName();
        OpenAPI openApi = document.getOpenApi();
        DocumentNode.Context documentContext = DocumentNode.Context.builder()
                .treePane(this)
                .document(document)
                .apiCounts(OpenApiUtils.countApi(openApi))
                .build();
        if (documentNode == null) {
            documentNode = new DocumentNode(AllIcons.Nodes.Module, documentContext);
        } else {
            documentNode.setData(documentContext);
        }

        if (openApi != null) {
            Map<String, FolderNode> folderNodes = new HashMap<>();
            @Nullable DocumentNode finalDocumentNode = documentNode;
            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    Request request = new Request(path, method.name(), operation);
                    RequestNode.Context requestContext = RequestNode.Context.builder()
                            .project(project)
                            .document(document)
                            .request(request)
                            .build();
                    RequestNode requestNode = new RequestNode(HttpMethodIcons.getHttpMethodIcon(method.name()), requestContext);
                    if (operation.getTags() != null && !operation.getTags().isEmpty()) {
                        String tag = operation.getTags().get(0);
                        FolderNode folderNode = folderNodes.computeIfAbsent(tag, key -> {
                            FolderNode.Context folderData = FolderNode.Context.builder()
                                    .folder(tag)
                                    .apiCount(OpenApiUtils.countApiByTag(openApi, tag))
                                    .build();
                            return new FolderNode(AllIcons.Nodes.Folder, folderData);
                        });
                        folderNode.add(requestNode);
                    } else {
                        finalDocumentNode.add(requestNode);
                    }

                    // for selects
                    String nodeId = getRequestNodeId(id, method.name(), path);
                    requestNodes.put(nodeId, requestNode);
                });
            });
            folderNodes.values().forEach(documentNode::add);
        }
        documentNodes.put(id, documentNode);
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
                // ignore
            }

            @Override
            public void mousePressed(MouseEvent event) {
                boolean isRightClick = SwingUtilities.isRightMouseButton(event);
                if (!isRightClick) {
                    return;
                }

                JPopupMenu popupMenu = null;
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (treeNode == null) {
                    return;
                }
                if (treeNode instanceof ApiViewNode) {
                    ApiViewNode.MouseEventContext ctx = ApiViewNode.MouseEventContext.builder()
                            .event(event)
                            .project(project)
                            .tree(tree)
                            .build();
                    popupMenu = ((ApiViewNode<?>) treeNode).getPopupMenu(ctx);
                }
                if (popupMenu != null) {
                    popupMenu.show(tree, event.getX(), event.getY());
                }
            }


        });

        // 键盘事件
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                ApiViewNode<?>[] nodes = tree.getSelectedNodes(ApiViewNode.class, null);
                if (nodes.length == 1) {
                    ApiViewNode.KeyEventContext ctx = ApiViewNode.KeyEventContext.builder()
                            .event(event)
                            .project(project)
                            .tree(tree)
                            .build();
                    nodes[0].keyPressed(ctx);
                    return;
                }
            }
        });
    }

    public void addMouseListener(MouseListener listener) {
        this.tree.addMouseListener(listener);
    }

    @Data
    public static class RenderArgs {
        private List<Document> documents;
    }

}
