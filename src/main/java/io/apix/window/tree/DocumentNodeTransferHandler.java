package io.apix.window.tree;

import com.intellij.openapi.project.Project;
import io.apix.document.DocumentRepository;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * Document node transfer handler.
 */
public class DocumentNodeTransferHandler extends TransferHandler {

    private static final DataFlavor DOCUMENT_NODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "DocumentNode");

    private final Project project;
    private final DefaultTreeModel treeModel;

    public DocumentNodeTransferHandler(Project project, DefaultTreeModel treeModel) {
        this.project = project;
        this.treeModel = treeModel;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (!(c instanceof JTree)) {
            return null;
        }
        JTree tree = (JTree) c;
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object node = path.getLastPathComponent();
        if (!(node instanceof DocumentNode)) {
            return null;
        }
        return new DocumentNodeTransferable((DocumentNode) node);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        if (!support.isDataFlavorSupported(DOCUMENT_NODE_FLAVOR)) {
            return false;
        }
        JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
        TreePath path = dropLocation.getPath();
        // Only allow drop at root level (path depth == 1 means dropping between root's children)
        if (path == null) {
            return false;
        }
        Object target = path.getLastPathComponent();
        return target == treeModel.getRoot();
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        DocumentNode draggedNode;
        try {
            draggedNode = (DocumentNode) support.getTransferable()
                    .getTransferData(DOCUMENT_NODE_FLAVOR);
        } catch (Exception e) {
            return false;
        }

        JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();

        int toIndex = dropLocation.getChildIndex();
        if (toIndex < 0) {
            toIndex = root.getChildCount();
        }

        int fromIndex = root.getIndex(draggedNode);
        if (fromIndex < 0 || fromIndex == toIndex || fromIndex + 1 == toIndex) {
            return false;
        }

        int insertIndex = toIndex > fromIndex ? toIndex - 1 : toIndex;

        DocumentNode targetNode = null;
        if (insertIndex < root.getChildCount()) {
            targetNode = (DocumentNode) root.getChildAt(insertIndex);
        }

        treeModel.removeNodeFromParent(draggedNode);
        treeModel.insertNodeInto(draggedNode, root, insertIndex);
        treeModel.nodeStructureChanged(root);

        String draggedId = draggedNode.getData().getDocument().getId();
        String targetId = targetNode != null
                ? targetNode.getData().getDocument().getId()
                : null;

        DocumentRepository.getInstance(project).reorder(draggedId, targetId);

        return true;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // Nothing to clean up; MOVE is handled in importData
    }

    private static class DocumentNodeTransferable implements Transferable {

        private final DocumentNode node;

        DocumentNodeTransferable(DocumentNode node) {
            this.node = node;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DOCUMENT_NODE_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DOCUMENT_NODE_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return node;
        }
    }
}
