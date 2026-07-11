package io.apix.icon;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class Icons {

    public static final Icon CODE = loadIcon("/icons/code.svg");

    static Icon loadIcon(String path) {
        return IconLoader.getIcon(path, Icons.class);
    }
}
