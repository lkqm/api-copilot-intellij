package io.apicopilot.window.tree;

import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

/**
 * Tree expander for selected.
 */
public class SelectedTreeExpander implements TreeExpander {

    private final JTree tree;
    private final DefaultTreeExpander treeExpander;

    public SelectedTreeExpander(@NotNull JTree tree) {
        this.tree = tree;
        this.treeExpander = new DefaultTreeExpander(tree);
    }

    @Override
    public void expandAll() {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath != null) {
            expandRecursively(selectedPath);
        } else {
            treeExpander.expandAll();
        }
    }

    @Override
    public void collapseAll() {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath != null) {
            collapseRecursively(selectedPath);
        } else {
            treeExpander.collapseAll();
        }
    }

    @Override
    public boolean canExpand() {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath != null) {
            return true;
        } else {
            return treeExpander.canExpand();
        }
    }

    @Override
    public boolean canCollapse() {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath != null) {
            return true;
        } else {
            return treeExpander.canCollapse();
        }
    }

    private void expandRecursively(TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            Enumeration<?> children = node.children();
            while (children.hasMoreElements()) {
                TreeNode child = (TreeNode) children.nextElement();
                TreePath path = parent.pathByAddingChild(child);
                expandRecursively(path);
            }
        }
        tree.expandPath(parent);
    }

    private void collapseRecursively(TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            Enumeration<?> children = node.children();
            while (children.hasMoreElements()) {
                TreeNode child = (TreeNode) children.nextElement();
                TreePath path = parent.pathByAddingChild(child);
                collapseRecursively(path);
            }
        }
        tree.collapsePath(parent);
    }
}
