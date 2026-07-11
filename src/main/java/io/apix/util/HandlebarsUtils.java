package io.apix.util;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HandlebarsUtils {

    public static Handlebars create() {
        Handlebars handlebars = new Handlebars();
        handlebars.registerHelpers(ConditionalHelpers.class);
        handlebars.registerHelpers(CustomHelpers.class);
        return handlebars;
    }

    @SneakyThrows
    public static String apply(String template, Object context) {
        Handlebars handlebars = create();
        Template template1 = handlebars.compileInline(template);
        return template1.apply(context);
    }

}
