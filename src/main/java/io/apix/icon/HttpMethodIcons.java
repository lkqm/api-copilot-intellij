package io.apix.icon;

import com.intellij.openapi.util.IconLoader;
import io.apix.model.HttpMethod;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class HttpMethodIcons {

    private static final Map<String, Icon> ICONS = new HashMap<>();

    static {
        for (HttpMethod method : HttpMethod.values()) {
            Icon icon = IconLoader.getIcon(String.format("/icons/method/%s.svg", method.name()), HttpMethodIcons.class);
            ICONS.put(method.name(), icon);
        }
    }

    @Nullable
    public static Icon getHttpMethodIcon(String method) {
        return ICONS.get(method);
    }

}