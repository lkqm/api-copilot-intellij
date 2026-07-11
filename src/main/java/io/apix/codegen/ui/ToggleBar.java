package io.apix.codegen.ui;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ToggleBar extends JPanel {
    private List<JLabel> labels = new ArrayList<>();
    private JLabel selectedLabel = null;
    private List<String> options = null;
    private Color activeColor;
    private ChangeListener changeListener;

    public ToggleBar() {
        this(Collections.emptyList());
    }

    public ToggleBar(List<String> options) {
        this.activeColor = UIManager.getColor("Component.borderColor");
        if (this.activeColor == null) {
            this.activeColor = JBColor.GRAY;
        }
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createLineBorder(activeColor, 1, true));
        setOptions(options, false);
    }

    public void setOptions(List<String> options, boolean selected) {
        this.options = options;
        refreshLabels(options);
        if (selected && this.labels != null && !this.labels.isEmpty()) {
            doSetSelectedLabel(this.labels.get(0));
        }
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public String getSelectedText() {
        return selectedLabel != null ? selectedLabel.getText() : null;
    }

    private void refreshLabels(List<String> options) {
        this.removeAll();

        this.labels.clear();
        for (int i = 0; i < options.size(); i++) {
            JLabel label = new JLabel(options.get(i), SwingConstants.CENTER);
            label.setOpaque(true);
            label.setAlignmentY(Component.CENTER_ALIGNMENT);
            label.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    doSetSelectedLabel(label);
                }
            });

            labels.add(label);
            add(label);
        }

        this.revalidate();
        this.repaint();
    }


    private void doSetSelectedLabel(JLabel label) {
        if (selectedLabel == label) {
            return;
        }
        if (selectedLabel != null) {
            selectedLabel.setBackground(null);
        }
        label.setBackground(activeColor);
        this.selectedLabel = label;
        if (this.changeListener != null) {
            changeListener.onChanged(label.getText());
        }
    }


    public interface ChangeListener {
        void onChanged(String value);
    }
}