package io.apicopilot.util;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;

public enum CustomHelpers implements Helper<Object> {

    camelCase {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            return toCamelCase(value.toString());
        }
    },
    pascalCase {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            return toPascalCase(value.toString());
        }
    },
    snakeCase {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            return toSnakeCase(value.toString());
        }
    },
    kebabCase {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            return toKebabCase(value.toString());
        }
    },
    upperCase {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            return value.toString().toUpperCase();
        }
    },
    lowerCase {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            return value.toString().toLowerCase();
        }
    },
    in {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            Object[] params = options.params;
            if (params == null) {
                return options.inverse(value);
            }

            for (Object param : params) {
                if (param != null && param.toString().equals(value != null ? value.toString() : null)) {
                    return options.fn(value);
                }
            }

            return options.inverse(value);
        }
    };

    private static String[] splitWords(String input) {
        String normalized = input
            .replace("-", " ")
            .replace("_", " ")
            .replaceAll("([a-z])([A-Z])", "$1 $2");
        return normalized.trim().split("\\s+");
    }

    private static String toCamelCase(String input) {
        String pascal = toPascalCase(input);
        if (pascal.isEmpty()) {
            return pascal;
        }
        return pascal.substring(0, 1).toLowerCase() + pascal.substring(1);
    }

    private static String toPascalCase(String input) {
        StringBuilder sb = new StringBuilder();
        for (String part : splitWords(input)) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase());
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private static String toSnakeCase(String input) {
        return String.join("_", splitWords(input)).toLowerCase();
    }

    private static String toKebabCase(String input) {
        return String.join("-", splitWords(input)).toLowerCase();
    }

    private static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
