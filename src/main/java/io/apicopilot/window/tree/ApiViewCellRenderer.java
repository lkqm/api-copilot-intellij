package io.apicopilot.window.tree;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import io.apicopilot.document.Document;
import io.apicopilot.document.SyncStatus;
import io.apicopilot.model.Request;
import io.apicopilot.util.TimeFormatUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.*;
import java.time.Instant;

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;

/**
 * 自定义ApiView树节点渲染器.
 */
public class ApiViewCellRenderer extends ColoredTreeCellRenderer {


    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof ApiViewNode) {
            ApiViewNode<?> node = (ApiViewNode<?>) value;
            if (getIcon() == null) {
                setIcon(node.getIcon());
            }
        }

        if (value instanceof DocumentNode) {
            // document node
            DocumentNode node = (DocumentNode) value;
            DocumentNode.Context data = node.getData();
            Document document = data.getDocument();
            setEnabled(document.isEnable());
            append(document.getName());
            SyncStatus syncStatus = document.getSyncStatus();
            if (syncStatus == SyncStatus.SYNCING) {
                append(" (loading...)", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
            }

            if (syncStatus != SyncStatus.SYNCING && selected && document.getLastSuccessTime() != null) {
                Instant loadTime = Instant.ofEpochMilli(document.getLastSuccessTime());
                String time = TimeFormatUtils.formatRelativeTime(loadTime);
                append(" · " + time, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
            }

            if (syncStatus == SyncStatus.FAILED) {
                append("  ⚠");
            }

        } else if (value instanceof FolderNode) {
            // folder node
            FolderNode node = (FolderNode) value;
            FolderNode.Context data = node.getData();
            append(data.getFolder());
        } else if (value instanceof RequestNode) {
            // request node
            RequestNode node = (RequestNode) value;
            RequestNode.Context data = node.getData();
            Request request = data.getRequest();
            String left = request.getOperation().getSummary();
            String right = request.getPath();
            if (StringUtils.isEmpty(left)) {
                left = request.getPath();
                right = null;
            }
            if (isNotTrue(request.getOperation().getDeprecated())) {
                append(left);
            } else {
                SimpleTextAttributes deleteAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());
                append(left, deleteAttributes);
            }
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

    }


}
