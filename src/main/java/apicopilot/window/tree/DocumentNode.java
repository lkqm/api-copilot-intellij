package apicopilot.window.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.swing.*;

public class DocumentNode extends ApiTreeNode<DocumentNode.Data> {

    public DocumentNode(Icon icon, @NonNull DocumentNode.Data data) {
        super(icon, data);
    }

    /**
     * Connection data.
     */
    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data {

        /**
         * connection id.
         */
        private String id;

        /**
         * connection name.
         */
        private String name;

        /**
         * api counts
         */
        private int apiCounts;

    }
}
