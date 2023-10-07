package apicopilot.window.tree;

import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

@Getter
public class ApiTreeNode<T> extends DefaultMutableTreeNode {
    protected final Icon icon;
    protected final T data;

    public ApiTreeNode(Icon icon, T data) {
        super(data);
        this.icon = icon;
        this.data = data;
    }

}