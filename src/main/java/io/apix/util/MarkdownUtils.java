package io.apix.util;

import lombok.experimental.UtilityClass;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Collections;
import java.util.List;

@UtilityClass
public class MarkdownUtils {

    private static final Parser parser;
    private static final HtmlRenderer renderer;

    static {
        List<Extension> extensions = Collections.singletonList(TablesExtension.create());
        parser = Parser.builder()
                .extensions(extensions)
                .build();
        renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
    }

    public static String toHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

}
