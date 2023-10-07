package apicopilot.window.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.swing.*;

public class FolderNode extends ApiTreeNode<FolderNode.Data> {

    public FolderNode(Icon icon, @NonNull FolderNode.Data data) {
        super(icon, data);
    }

    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data {

        /**
         * 标签
         */
        private String tag;
    }
}
