package io.apix.util;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class TreeUtils {

    public static <T> List<T> getSelectedNodes(JTree tree, Class<T> type) {
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if(selectionPaths == null || selectionPaths.length == 0){
            return Collections.emptyList();
        }

        return Arrays.stream(selectionPaths).map(TreePath::getLastPathComponent)
                .filter(node -> node != null && node.getClass().isAssignableFrom(type))
                .map(node -> (T) node)
                .collect(Collectors.toList());
    }

    public static <T> List<T> getChild(DefaultMutableTreeNode parent, Class<T> clazz) {
        List<T> results = new ArrayList<>();

        Enumeration<?> enumeration = parent.depthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            Object nodeObj = enumeration.nextElement();
            if (nodeObj == parent) continue;

            if (nodeObj.getClass().isAssignableFrom(clazz)) {
                results.add((T) nodeObj);
            }
        }

        return results;
    }

}
