package io.apicopilot.window.tree;

import lombok.*;

import javax.swing.*;

/**
 * Folder node.
 */
public class FolderNode extends ApiViewNode<FolderNode.Context> {

    public FolderNode(Icon icon, @NonNull FolderNode.Context data) {
        super(icon, data);
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Context {

        private String folder;

        private int apiCount;
    }
}
