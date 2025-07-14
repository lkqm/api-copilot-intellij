package io.apicopilot.window.tree;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import io.apicopilot.codegen.ui.GenerateModelDialog;
import io.apicopilot.codegen.ui.GenerateRequestDialog;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.ClipboardUtils;
import io.apicopilot.util.MarkdownGenerator;
import io.apicopilot.util.OpenApiUtils;
import io.swagger.v3.oas.models.Operation;
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
        JPopupMenu popupMenu = new JBPopupMenu();
        JMenuItem copyPathItem = new JBMenuItem("Copy Path");
        copyPathItem.addActionListener(actionEvent -> {
            ClipboardUtils.setClipboard(data.getRequest().getPath());
        });
        popupMenu.add(copyPathItem);

        // Copy Request As Json
        Operation operation = data.getRequest().getOperation();
        JMenuItem copyRequestJsonItem = new JBMenuItem("Copy Request Json");
        copyRequestJsonItem.addActionListener(actionEvent -> {
            String text = OpenApiUtils.buildRequestJson(data.getRequest().getOperation());
            ClipboardUtils.setClipboard(text);
        });
        copyRequestJsonItem.setEnabled(OpenApiUtils.hasRequestBody(operation));
        popupMenu.add(copyRequestJsonItem);

        // Copy Response As Json
        JMenuItem copyResponseJsonItem = new JBMenuItem("Copy Response Json");
        copyResponseJsonItem.addActionListener(actionEvent -> {
            String text = OpenApiUtils.buildResponseJson(operation.getResponses());
            ClipboardUtils.setClipboard(text);
        });
        copyResponseJsonItem.setEnabled(OpenApiUtils.hasResponseBody(operation));
        popupMenu.add(copyResponseJsonItem);

        // Copy as Markdown
        JMenuItem copyMarkdownItem = new JBMenuItem("Copy as Markdown");
        copyMarkdownItem.addActionListener(actionEvent -> {
            String text = new MarkdownGenerator().generate(this.data.getRequest());
            ClipboardUtils.setClipboard(text);
        });
        popupMenu.add(copyMarkdownItem);

        // Generate Model Code
        JMenuItem generateModelItem = new JBMenuItem("Generate Model Code");
        generateModelItem.addActionListener(actionEvent -> {
            GenerateModelDialog dialog = new GenerateModelDialog(this.data.getProject(), this.data.getDocument(), this.data.getRequest());
            dialog.show();
        });
        popupMenu.add(generateModelItem);

        // Generate Request Code
        JMenuItem generateRequestItem = new JBMenuItem("Generate Request Code");
        generateRequestItem.addActionListener(actionEvent -> {
            GenerateRequestDialog dialog = new GenerateRequestDialog(this.data.getProject(), this.data.getDocument(), this.data.getRequest());
            dialog.show();
        });
        popupMenu.add(generateRequestItem);


        return popupMenu;
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
    }
}
