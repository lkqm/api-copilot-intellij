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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;

/**
 * 自定义ApiView树节点渲染器.
 */
public class ApiViewCellRenderer extends ColoredTreeCellRenderer {


    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        setToolTipText(null);

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
            boolean loading = syncStatus == SyncStatus.SYNCING;
            boolean failed = syncStatus == SyncStatus.FAILED;
            boolean hasUpdate = document.isHasUpdate();

            if (loading) {
                append(" · Syncing", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
            } else if (selected) {
                if (document.getLastSuccessTime() != null) {
                    Instant loadTime = Instant.ofEpochMilli(document.getLastSuccessTime());
                    String time = TimeFormatUtils.formatRelativeTime(loadTime);
                    append(" · Synced " + time, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
                } else {
                    append(" · Not synced", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
                }
            }
            appendStatusIcons(failed, hasUpdate);
            setToolTipText(buildDocumentTooltip(document, loading, failed, hasUpdate));

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

    private void appendStatusIcons(boolean failed, boolean hasUpdate) {
        if (failed) {
            append("  ⚠", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        if (hasUpdate) {
            append("  ↻", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    private String buildDocumentTooltip(Document document, boolean loading, boolean failed, boolean hasUpdate) {
        if (!failed && !hasUpdate) {
            return null;
        }

        List<String> lines = new ArrayList<>();
        if (failed) {
            lines.add("Sync failed");
        }
        if (hasUpdate) {
            lines.add("Update available");
        }
        if (document.getLastSuccessTime() != null) {
            Instant loadTime = Instant.ofEpochMilli(document.getLastSuccessTime());
            lines.add("Last synced: " + TimeFormatUtils.formatDateTime(loadTime));
        } else if (failed || hasUpdate) {
            lines.add("No successful sync yet");
        }

        if (lines.isEmpty()) {
            return null;
        }
        return "<html>" + String.join("<br/>", lines) + "</html>";
    }


}
