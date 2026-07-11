package io.apix.window.action;


import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import io.apix.codegen.ui.GenerateCodeDialog;
import io.apix.document.Document;
import io.apix.icon.Icons;
import io.apix.model.Request;
import io.apix.util.TreeUtils;
import io.apix.window.ApiView;
import io.apix.window.tree.DocumentNode;
import io.apix.window.tree.FolderNode;
import io.apix.window.tree.RequestNode;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generate Code Action
 */
public class GenerateCodeAction extends AnAction {

    public GenerateCodeAction() {
        getTemplatePresentation().setText("Generate Code");
        getTemplatePresentation().setIcon(Icons.CODE);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ApiView apiView = ApiView.getInstance(project);
        Tree tree = apiView.getPanel().getTreePane().getTree();
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths == null || selectionPaths.length == 0) {
            return;
        }

        List<RequestNode> nodes = new ArrayList<>();

        List<DocumentNode> documentNodes = TreeUtils.getSelectedNodes(tree, DocumentNode.class);
        List<FolderNode> folderNodes = TreeUtils.getSelectedNodes(tree, FolderNode.class);
        List<RequestNode> requestNodes = TreeUtils.getSelectedNodes(tree, RequestNode.class);
        if (CollectionUtils.isNotEmpty(documentNodes)) {
            for (DocumentNode documentNode : documentNodes) {
                List<RequestNode> child = TreeUtils.getChild(documentNode, RequestNode.class);
                nodes.addAll(child);
            }
        }
        if (CollectionUtils.isNotEmpty(folderNodes)) {
            for (FolderNode folderNode : folderNodes) {
                List<RequestNode> child = TreeUtils.getChild(folderNode, RequestNode.class);
                if (!child.isEmpty()) {
                    Set<RequestNode> nodesSet = Sets.newHashSet(nodes);
                    nodes.addAll(child.stream().filter(node -> !nodesSet.contains(node)).collect(Collectors.toSet()));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(requestNodes)) {
            Set<RequestNode> nodesSet = Sets.newHashSet(nodes);
            nodes.addAll(requestNodes.stream().filter(node -> !nodesSet.contains(node)).collect(Collectors.toSet()));
        }
        if (nodes.isEmpty()) {
            return;
        }
        Set<Document> documents = nodes.stream().map(node -> node.getData().getDocument()).collect(Collectors.toSet());
        if (documents.size() > 1) {
            return;
        }
        List<Request> requests = nodes.stream().map(node -> node.getData().getRequest()).collect(Collectors.toList());

        RequestNode.Context data = nodes.get(0).getData();
        GenerateCodeDialog dialog = new GenerateCodeDialog(data.getProject(), data.getDocument(), requests);
        dialog.show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ApiView apiView = ApiView.getInstance(project);
        Tree tree = apiView.getPanel().getTreePane().getTree();
        TreePath[] selectionPaths = tree.getSelectionPaths();
        boolean canGenerate = selectionPaths != null && selectionPaths.length > 0;
        e.getPresentation().setEnabled(canGenerate);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
