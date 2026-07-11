package io.apix.util;

/**
 * 命名工具类
 */
public class NamedUtils {

    /**
     * 小驼峰命名（首字母小写，单词间大写字母分割）
     */
    public static String toCamelCase(String input) {
        String pascal = toPascalCase(input);
        if (pascal.isEmpty()) {
            return pascal;
        }
        return pascal.substring(0, 1).toLowerCase() + pascal.substring(1);
    }

    /**
     * 帕斯卡命名（首字母大写，单词间大写字母分割）
     */
    public static String toPascalCase(String input) {
        StringBuilder sb = new StringBuilder();
        for (String part : splitWords(input)) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase());
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * 蛇型命名（单词小写，用下划线分割）
     */
    public static String toSnakeCase(String input) {
        return String.join("_", splitWords(input)).toLowerCase();
    }

    /**
     * 烤串命名（单词小写，用中划线分割）
     */
    public static String toKebabCase(String input) {
        return String.join("-", splitWords(input)).toLowerCase();
    }

    private static String[] splitWords(String input) {
        String normalized = input
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([0-9])([a-zA-Z])", "$1 $2")
                .replaceAll("([a-zA-Z])([0-9])", "$1 $2");
        return normalized.trim().split("\\s+");
    }

}
