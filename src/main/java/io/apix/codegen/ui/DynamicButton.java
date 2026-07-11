package io.apix.codegen.ui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

public class DynamicButton extends JButton {

    private String text;
    private Icon icon;
    private String triggerText;
    private Icon triggerIcon;
    private Timer resetTimer;
    private long delay;

    public DynamicButton(String text, Icon icon, String triggerText, Icon triggerIcon) {
        super(text, icon);
        this.text = text;
        this.icon = icon;
        this.delay = 2000;
        this.triggerText = triggerText;
        this.triggerIcon = triggerIcon;
    }

    @Override
    public void addActionListener(ActionListener l) {
        super.addActionListener(e -> {
            l.actionPerformed(e);

            if (resetTimer != null) {
                resetTimer.cancel();
            }
            DynamicButton.this.setIcon(triggerIcon);
            DynamicButton.this.setText(triggerText);
            resetTimer = new Timer();
            resetTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> {
                        DynamicButton.this.setText(text);
                        DynamicButton.this.setIcon(icon);
                        resetTimer.cancel();
                    });
                }
            }, delay);
        });
    }
}
