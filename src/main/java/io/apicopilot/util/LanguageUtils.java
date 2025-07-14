package io.apicopilot.util;

import com.intellij.openapi.application.ApplicationInfo;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class LanguageUtils {

    public static String getDefaultLanguage(List<String> supports, String defaultLanguage) {
        String language = getDefaultLanguage();
        if (language != null && !supports.contains(language)) {
            language = null;
        }
        if (language == null) {
            language = defaultLanguage;
        }
        return language;
    }

    public static String getDefaultLanguage() {
        // 使用 ApplicationInfo 来检测 IDE 类型，这是更稳定的 API
        String versionName = ApplicationInfo.getInstance().getVersionName();

        if (versionName.contains("WebStorm")) {
            return "TypeScript";
        } else if (versionName.contains("IntelliJ IDEA")) {
            return "Java";
        } else if (versionName.contains("PyCharm")) {
            return "Python";
        } else if (versionName.contains("PhpStorm")) {
            return "PHP";
        } else if (versionName.contains("GoLand")) {
            return "Go";
        } else if (versionName.contains("RubyMine")) {
            return "Ruby";
        } else if (versionName.contains("CLion")) {
            return "C++";
        } else if (versionName.contains("DataGrip")) {
            return "SQL";
        } else if (versionName.contains("Rider")) {
            return "C#";
        } else if (versionName.contains("Android Studio")) {
            return "Kotlin";
        }

        return null;
    }
}
