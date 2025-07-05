package io.apicopilot.window;


import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.apicopilot.util.MarkdownGenerator;
import io.apicopilot.util.ResourceUtils;
import lombok.Getter;


/**
 * Preview API detail panel.
 */
public class ApiViewPreviewPane extends JBScrollPane {

    private final JCEFHtmlPanel htmlPanel;
    private static final String API_HTML_TPL_FILE = "api_preview_tpl.html";
    @Getter
    private Document document;
    @Getter
    private Request request;

    public ApiViewPreviewPane() {
        this.htmlPanel = new JCEFHtmlPanel(null);
        this.add(htmlPanel.getComponent());
        this.setViewportView(htmlPanel.getComponent());

        // 根据主题适配网页内容
        ApplicationManager.getApplication().getMessageBus()
                .connect()
                .subscribe(LafManagerListener.TOPIC, (LafManagerListener) source -> {
                    String theme = JBColor.isBright() ? "light" : "dark";
                    htmlPanel.getCefBrowser().executeJavaScript("setTheme('" + theme + "');", htmlPanel.getCefBrowser().getURL(), 0);
                });

    }

    public void setRequest(Document document, Request request) {
        String tpl = ResourceUtils.readAsTextWithCache(API_HTML_TPL_FILE);
        String content = new MarkdownGenerator().generateHtml(request);
        String theme = JBColor.isBright() ? "light" : "dark";
        String html = String.format(tpl, theme, content);
        htmlPanel.loadHTML(html);

        this.document = document;
        this.request = request;
    }


}
