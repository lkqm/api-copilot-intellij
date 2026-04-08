package io.apicopilot.window.debug;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * A tab label component that shows a small colored dot when the tab has values,
 * similar to Postman's tab indicator.
 */
class DotTabLabel extends JPanel {

    private static final Color DOT_COLOR = new JBColor(new Color(0x5BAD7F), new Color(0x4CAF7D));
    private static final int DOT_SIZE    = 5;
    private static final int DOT_GAP     = 3;

    private final JLabel label;
    private boolean dotVisible = false;

    DotTabLabel(String text) {
        super(null);
        setOpaque(false);
        label = new JLabel(text);
        add(label);
    }

    void setDotVisible(boolean visible) {
        if (this.dotVisible == visible) return;
        this.dotVisible = visible;
        revalidate();
        repaint();
    }

    boolean isDotVisible() {
        return dotVisible;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ld = label.getPreferredSize();
        int extra = dotVisible ? JBUI.scale(DOT_SIZE + DOT_GAP) : 0;
        return new Dimension(ld.width + extra, ld.height);
    }

    @Override
    public void doLayout() {
        label.setBounds(0, 0, label.getPreferredSize().width, getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!dotVisible) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DOT_COLOR);
        int size = JBUI.scale(DOT_SIZE);
        int x = label.getPreferredSize().width + JBUI.scale(DOT_GAP);
        int y = (getHeight() - size) / 2;
        g2.fillOval(x, y, size, size);
        g2.dispose();
    }
}
