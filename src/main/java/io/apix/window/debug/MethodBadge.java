package io.apix.window.debug;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Colored text label showing the HTTP method (GET / POST / PUT …).
 * No background — method color is applied to the text foreground only.
 */
public class MethodBadge extends JLabel {

    public MethodBadge() {
        super("GET");
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(getFont().deriveFont(Font.BOLD));
        setBorder(JBUI.Borders.empty(0, 4, 0, 6));
        setForeground(methodColor("GET"));
    }

    public void setMethod(String method) {
        String m = method != null ? method.toUpperCase() : "GET";
        setText(m);
        setForeground(methodColor(m));
    }

    private static Color methodColor(String method) {
        switch (method) {
            case "POST":    return new JBColor(new Color(0x0070BB), new Color(0x2196F3));
            case "PUT":     return new JBColor(new Color(0xD97706), new Color(0xFFA726));
            case "PATCH":   return new JBColor(new Color(0x7C3AED), new Color(0xAB47BC));
            case "DELETE":  return new JBColor(new Color(0xDC2626), new Color(0xEF5350));
            case "HEAD":
            case "OPTIONS": return new JBColor(new Color(0x6B7280), new Color(0x9E9E9E));
            default:        return new JBColor(new Color(0x059669), new Color(0x4CAF50)); // GET
        }
    }
}
