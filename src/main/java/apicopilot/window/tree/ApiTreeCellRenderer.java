package apicopilot.window.tree;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 自定义渲染树节点
 */
public class ApiTreeCellRenderer extends ColoredTreeCellRenderer {

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof DocumentNode) {
            // connection node
            DocumentNode node = (DocumentNode) value;
            DocumentNode.Data data = node.getData();
            append(data.getName());
        } else if (value instanceof FolderNode) {
            // folder node
            FolderNode node = (FolderNode) value;
            FolderNode.Data data = node.getData();
            append(data.getTag());
        } else if (value instanceof RequestNode) {
            // api node
            RequestNode node = (RequestNode) value;
            RequestNode.Data data = node.getData();
            String left = data.getRequest().getOperation().getSummary();
            String right = data.getRequest().getPath();
            if (StringUtils.isEmpty(left)) {
                left = data.getRequest().getPath();
                right = null;
            }
            append(left);
            if (right != null) {
                append(" " + right, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        } else if (value instanceof DefaultMutableTreeNode) {
            // default
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (node.getUserObject() != null) {
                append(node.getUserObject().toString());
            }
        }

        if (value instanceof ApiTreeNode) {
            ApiTreeNode<?> node = (ApiTreeNode<?>) value;
            if (getIcon() == null) {
                setIcon(node.getIcon());
            }
        }
    }
}
