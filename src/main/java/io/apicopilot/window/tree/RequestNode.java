package io.apicopilot.window.tree;

import com.intellij.openapi.project.Project;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.ClipboardUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Request node.
 */
public class RequestNode extends ApiViewNode<RequestNode.Context> {

    public RequestNode(Icon icon, @NonNull RequestNode.Context data) {
        super(icon, data);
    }

    @Override
    public JPopupMenu getPopupMenu(MouseEventContext ctx) {
        return null;
    }

    @Override
    public void keyPressed(KeyEventContext ctx) {
        KeyEvent event = ctx.getEvent();

        // 打开API详情
        if (event.getKeyCode() == KeyEvent.VK_ENTER) {
            // TODO: 回车打开对于API文档详情
            return;
        }

        // 复制API路径
        if (event.getKeyCode() == KeyEvent.VK_C && (event.isControlDown() || event.isMetaDown())) {
            String text = this.getData().getRequest().getPath();
            ClipboardUtils.setClipboard(text);
            return;
        }
    }

    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Context {

        @NonNull
        private Project project;

        @NonNull
        private Request request;

        @NonNull
        private Document document;

        @NonNull
        private JTree tree;
    }
}
