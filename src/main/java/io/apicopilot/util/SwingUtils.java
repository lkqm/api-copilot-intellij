package io.apicopilot.util;

import lombok.experimental.UtilityClass;

import javax.swing.*;

@UtilityClass
public class SwingUtils {

    public static Object getComponentValue(JComponent comp) {
        if (comp instanceof JTextField) {
            return ((JTextField) comp).getText();

        } else if (comp instanceof JPasswordField) {
            return new String(((JPasswordField) comp).getPassword());

        } else if (comp instanceof JTextArea) {
            return ((JTextArea) comp).getText();

        } else if (comp instanceof JCheckBox) {
            return ((JCheckBox) comp).isSelected();

        } else if (comp instanceof JRadioButton) {
            return ((JRadioButton) comp).isSelected();

        } else if (comp instanceof JComboBox) {
            return ((JComboBox<?>) comp).getSelectedItem();

        } else if (comp instanceof JSpinner) {
            return ((JSpinner) comp).getValue();

        } else if (comp instanceof JSlider) {
            return ((JSlider) comp).getValue();

        } else if (comp instanceof JFormattedTextField) {
            return ((JFormattedTextField) comp).getValue();

        } else if (comp instanceof JTextPane) {
            return ((JTextPane) comp).getText();

        } else if (comp instanceof JEditorPane) {
            return ((JEditorPane) comp).getText();

        } else {
            System.err.println("⚠️ 不支持的组件类型: " + comp.getClass().getName());
            return null;
        }
    }
}
