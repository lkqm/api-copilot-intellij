package io.apix.window.tree;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Base API view node.
 */
@Getter
@Setter
public abstract class ApiViewNode<T> extends DefaultMutableTreeNode {
    protected Icon icon;
    protected T data;

    public ApiViewNode(Icon icon, T data) {
        this.icon = icon;
        this.data = data;
    }

    public JPopupMenu getPopupMenu(MouseEventContext ctx) {
        return null;
    }

    public void keyPressed(KeyEventContext ctx) {
    }


    @Data
    @Builder
    public static class MouseEventContext {
        private final MouseEvent event;
        private final Project project;
        private final Tree tree;
    }

    @Data
    @Builder
    public static class KeyEventContext {
        private final KeyEvent event;
        private final Project project;
        private final Tree tree;
    }
}