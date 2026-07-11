package io.apix.util;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public enum CustomHelpers implements Helper<Object> {

    camel {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if (value == null) {
                return "";
            }
            return NamedUtils.toCamelCase(value.toString());
        }
    },
    pascal {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if (value == null) {
                return "";
            }
            return NamedUtils.toPascalCase(value.toString());
        }
    },
    snake {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if (value == null) {
                return "";
            }
            return NamedUtils.toSnakeCase(value.toString());
        }
    },
    kebab {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if (value == null) {
                return "";
            }
            return NamedUtils.toKebabCase(value.toString());
        }
    },
    upper {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if (value == null) {
                return "";
            }
            return value.toString().toUpperCase();
        }
    },
    lower {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if (value == null) {
                return "";
            }
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
    },
    replace {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            String target = options.param(0, null);
            String replacement = options.param(1, null);
            String result = value.toString().replace(target, replacement);
            return new Handlebars.SafeString(result);
        }
    },
    removeStart {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if(value == null) {
                return "";
            }
            String target = options.param(0, null);
            return StringUtils.removeStart(value.toString(), target);
        }
    },
    toJsModule {
        @Override
        public Object apply(Object value, Options options) throws IOException {
            if (value == null) return "";
            String path = value.toString().replace("\\", "/");
            if (path.startsWith("src/")) {
                path = "@" + path.substring(3);
            }
            int lastDot = path.lastIndexOf('.');
            if (lastDot > path.lastIndexOf('/')) {
                path = path.substring(0, lastDot);
            }
            return new Handlebars.SafeString(path);
        }
    },
    toSimpleClassName {
        @Override
        public CharSequence apply(Object value, Options options) throws IOException {
            if (value == null) {
                return "";
            }
            String name = value.toString();
            int lastDot = name.lastIndexOf('.');
            return lastDot >= 0 ? name.substring(lastDot + 1) : name;
        }
    }
    ;
}
