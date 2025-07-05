package io.apicopilot.icon;

import com.intellij.openapi.util.IconLoader;
import io.apicopilot.codegen.core.GenerateConfigs;
import io.apicopilot.codegen.model.Language;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class LanguageIcons {

    private static final Map<String, Icon> ICONS = new HashMap<>();

    static {
        // 从配置中获取所有支持的语言
        List<Language> languages = GenerateConfigs.getInstance().getConfig().getLanguages();
        for (Language language : languages) {
            String languageName = language.getLanguage();
            String iconPath = getIconPath(languageName);
            Icon icon = IconLoader.getIcon(iconPath, LanguageIcons.class);
            if (icon != null) {
                ICONS.put(languageName, icon);
            }
        }
    }

    private static String getIconPath(String language) {
        return String.format("/icons/languages/%s.svg", language.toLowerCase());
    }

    @Nullable
    public static Icon getLanguageIcon(String language) {
        return ICONS.get(language);
    }
}