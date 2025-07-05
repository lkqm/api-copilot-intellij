package io.apicopilot.util;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HandlebarsUtils {

    public static Handlebars create() {
        Handlebars handlebars = new Handlebars();
        handlebars.registerHelpers(ConditionalHelpers.class);
        handlebars.registerHelpers(StringHelpers.class);
        handlebars.registerHelpers(CustomHelpers.class);
        return handlebars;
    }

}
