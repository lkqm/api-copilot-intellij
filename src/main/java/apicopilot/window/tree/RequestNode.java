package apicopilot.window.tree;

import apicopilot.model.Request;
import apicopilot.util.ClipboardUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.swing.*;

public class RequestNode extends ApiTreeNode<RequestNode.Data> {

    public RequestNode(Icon icon, @NonNull RequestNode.Data data) {
        super(icon, data);
    }

    public JPopupMenu getPopupMenu() {
        JMenuItem copyPathItem = new JBMenuItem("Copy Path", AllIcons.Actions.Copy);
        copyPathItem.addActionListener(actionEvent -> {
            ClipboardUtils.setClipboard(data.getRequest().getPath());
        });

        JMenuItem copyJsonItem = new JBMenuItem("Copy Request Json", AllIcons.Actions.Copy);
        copyJsonItem.addActionListener(actionEvent -> {
            ClipboardUtils.setClipboard(data.getRequest().getPath());
        });

        JPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.add(copyPathItem);
        popupMenu.add(copyJsonItem);
        return popupMenu;
    }

    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data {

        @NonNull
        private Request request;

    }
}
