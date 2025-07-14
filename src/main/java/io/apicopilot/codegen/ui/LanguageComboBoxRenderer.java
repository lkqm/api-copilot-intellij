package io.apicopilot.codegen.ui;

import com.intellij.ui.components.JBLabel;
import io.apicopilot.icon.LanguageIcons;

import javax.swing.*;
import java.awt.*;

public class LanguageComboBoxRenderer extends JBLabel implements ListCellRenderer<String> {

    public LanguageComboBoxRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {

        // 设置文本
        setText(value);

        // 设置图标
        Icon icon = LanguageIcons.getLanguageIcon(value);
        setIcon(icon);

        // 设置图标和文本之间的间距
        setIconTextGap(6);

        // 设置选中和未选中状态的颜色
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}