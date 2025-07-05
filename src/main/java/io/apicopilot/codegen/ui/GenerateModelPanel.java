package io.apicopilot.codegen.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import io.apicopilot.codegen.ui.LanguageComboBoxRenderer;
import io.apicopilot.codegen.core.GenerateConfigs;
import io.apicopilot.codegen.generator.ModelCodeGenerator;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 生成模型界面
 */
public class GenerateModelPanel extends JBPanel<GenerateModelPanel> implements Disposable {

    private final Project project;
    private JComboBox<String> languageComboBox;
    private CodeEditorPanel codeEditor;
    private JButton copyButton;
    private final ModelCodeGenerator codeGenerator;
    private Timer resetTimer;

    public GenerateModelPanel(Project project, Document document, Request request) {
        super(new BorderLayout());
        this.project = project;
        this.codeGenerator = new ModelCodeGenerator(document, request);

        setPreferredSize(new Dimension(880, 420));
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        JBSplitter splitter = new JBSplitter(false, "GenerateModelPanelSplitter", 0.28f);
        splitter.setFirstComponent(leftPanel);
        splitter.setSecondComponent(rightPanel);
        add(splitter);

        copyButton.addActionListener(this::handleCopy);
        languageComboBox.addActionListener(this::handleLanguageChange);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setPreferredSize(new Dimension(270, 420));

        // 语言选择区域
        JPanel languagePanel = new JPanel(new BorderLayout());
        languagePanel.setBorder(BorderFactory.createTitledBorder(""));
        List<String> languages = GenerateConfigs.getInstance().getConfig().getModelLanguages();
        languageComboBox = new ComboBox<>(languages.toArray(new String[0]));
        
        // 设置自定义渲染器以显示语言图标
        languageComboBox.setRenderer(new LanguageComboBoxRenderer());
        
        new ComboboxSpeedSearch(languageComboBox);
        languagePanel.add(languageComboBox, BorderLayout.CENTER);

        // 参数配置区域
        JPanel parameterContainer = new JPanel(new BorderLayout());
        parameterContainer.setBorder(BorderFactory.createTitledBorder(""));
        JPanel parameterPanel = new JPanel();
        parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
        parameterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JBScrollPane(parameterPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        parameterContainer.add(scrollPane, BorderLayout.CENTER);

        leftPanel.add(languagePanel, BorderLayout.NORTH);
        leftPanel.add(parameterContainer, BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));

        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        copyButton = new JButton("Copy", AllIcons.Actions.Copy);
        buttonPanel.add(copyButton);

        // 编辑器区域
        codeEditor = new CodeEditorPanel(this.project);

        rightPanel.add(buttonPanel, BorderLayout.NORTH);
        rightPanel.add(codeEditor, BorderLayout.CENTER);
        return rightPanel;
    }

    public void setLanguage(String language) {
        languageComboBox.setSelectedItem(language);
    }

    private void handleCopy(ActionEvent e) {
        String code = codeEditor.getText();
        StringSelection selection = new StringSelection(code);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

        // 改变按钮图标和文本
        copyButton.setIcon(AllIcons.Actions.Checked);
        copyButton.setText("Copied");

        // 取消之前的定时器
        if (resetTimer != null) {
            resetTimer.cancel();
        }

        // 2秒后恢复原始状态
        resetTimer = new Timer();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    copyButton.setIcon(AllIcons.Actions.Copy);
                    copyButton.setText("Copy");
                });
            }
        }, 2000);
    }

    private void handleLanguageChange(ActionEvent e) {
        String language = (String) languageComboBox.getSelectedItem();
        if (language == null) {
            return;
        }

        String code = codeGenerator.generateCode(language);
        String extension = GenerateConfigs.getInstance().getConfig().getExtension(language);
        codeEditor.setText(code, extension);
    }

    @Override
    public void dispose() {
        if (resetTimer != null) {
            resetTimer.cancel();
        }
        codeEditor.dispose();
    }
}
