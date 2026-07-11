package io.apix.codegen.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import io.apix.codegen.CodegenSettings;
import io.apix.codegen.core.GenerateConfigs;
import io.apix.codegen.generator.ModelCodeGenerator;
import io.apix.codegen.model.GenerateModelTarget;
import io.apix.codegen.model.ModelTemplate;
import io.apix.document.Document;
import io.apix.model.Request;
import io.apix.util.SwingUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生成模型界面
 */
public class GenerateModelPanel extends JBPanel<GenerateModelPanel> implements Disposable {

    private final Project project;
    private JComboBox<String> languageComboBox;
    private CodeEditorPanel codeEditor;
    private JButton copyButton;
    private final ModelCodeGenerator codeGenerator;
    private JPanel optionsPanel;
    private Map<String, JComponent> optionsComponents = new HashMap<>();
    private ModelTemplate template;

    public GenerateModelPanel(Project project, Document document, Request request) {
        this(project, document, request, GenerateModelTarget.all());
    }

    public GenerateModelPanel(Project project, Document document, Request request, GenerateModelTarget target) {
        super(new BorderLayout());
        this.project = project;
        this.codeGenerator = new ModelCodeGenerator(document, request, target);

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

    @Override
    public void dispose() {
        codeEditor.dispose();
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
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JBScrollPane(optionsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        leftPanel.add(languagePanel, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));

        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        copyButton = new DynamicButton("Copy", AllIcons.Actions.Copy, "Copied", AllIcons.Actions.Checked);
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
    }

    private void handleLanguageChange(ActionEvent e) {
        String language = (String) languageComboBox.getSelectedItem();
        if (language == null) {
            return;
        }
        if (project != null) {
            CodegenSettings.getInstance(project).modelLastLanguage = language;
        }
        rebuildOptionsPanel(language);
        doGenerateCode();
    }

    private void doGenerateCode() {
        String language = (String) languageComboBox.getSelectedItem();
        if (language == null) {
            return;
        }

        Map<String, Object> optionValues = getOptionValues();
        String code = codeGenerator.generateCode(language, optionValues);
        String extension = GenerateConfigs.getInstance().getConfig().getExtension(language);
        codeEditor.setText(code, extension);
    }

    private void rebuildOptionsPanel(String language) {
        optionsPanel.removeAll();
        optionsComponents.clear();
        template = GenerateConfigs.getInstance().getConfig().getModelTemplate(language);
        if (template == null) {
            optionsPanel.revalidate();
            optionsPanel.repaint();
            return;
        }

        List<ModelTemplate.Option> options = template.getOptions();
        if (CollectionUtils.isEmpty(options)) {
            optionsPanel.revalidate();
            optionsPanel.repaint();
            return;
        }
        for (ModelTemplate.Option option : options) {
            if(BooleanUtils.isTrue(option.getHidden())) {
                continue;
            }
            optionsPanel.add(Box.createVerticalStrut(8));
            if ("text".equals(option.getType())) {
                String label = option.getLabel();
                if (StringUtils.isNotEmpty(label)) {
                    JLabel labelComponent = new JLabel(" " + label);
                    labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
                    labelComponent.setAlignmentY(Component.CENTER_ALIGNMENT);
                    JPanel labelPanel = linePanel(labelComponent);
                    optionsPanel.add(labelPanel);
                }
                String defaultValue = option.getDefaultValue() != null?option.getDefaultValue().toString():null;
                JTextField component = new JTextField(defaultValue);
                component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
                JPanel fieldPanel = linePanel(component);
                optionsComponents.put(option.getName(), component);
                optionsPanel.add(fieldPanel);
                component.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        doGenerateCode();
                    }
                });

            } else if ("list".equals(option.getType())) {
                String label = option.getLabel();
                if (StringUtils.isNotEmpty(label)) {
                    JLabel labelComponent = new JLabel(" " + label);
                    labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
                    labelComponent.setAlignmentY(Component.CENTER_ALIGNMENT);
                    JPanel labelPanel = linePanel(labelComponent);
                    optionsPanel.add(labelPanel);
                }
                List<String> values = option.getValues().stream().map(ModelTemplate.Value::getValue)
                        .collect(Collectors.toList());
                JComboBox<String> component = new ComboBox<>(values.toArray(new String[0]));
                component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
                component.setSelectedItem(option.getDefaultValue());
                JPanel comboPanel = linePanel(component);
                optionsComponents.put(option.getName(), component);
                optionsPanel.add(comboPanel);
                component.addActionListener((e) -> doGenerateCode());
            } else if ("boolean".equals(option.getType())) {
                JBCheckBox component = new JBCheckBox(option.getLabel(), "true".equals(option.getDefaultValue()));
                JPanel checkPanel = linePanel(component);
                optionsComponents.put(option.getName(), component);
                optionsPanel.add(checkPanel);
                component.addChangeListener((e) -> doGenerateCode());
            }
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private JPanel linePanel(JComponent component) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(component);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return panel;
    }

    private Map<String, Object> getOptionValues() {
        List<ModelTemplate.Option> options = template.getOptions();
        if(CollectionUtils.isEmpty(options)) {
            return Collections.emptyMap();
        }

        Map<String, Object> values = new HashMap<>();
        for (ModelTemplate.Option option : options) {
            if(BooleanUtils.isTrue(option.getHidden())) {
                values.put(option.getName(), option.getDefaultValue());
            } else if(optionsComponents.containsKey(option.getName())) {
                JComponent component = optionsComponents.get(option.getName());
                Object value = SwingUtils.getComponentValue(component);
                values.put(option.getName(), value);
            }
        }
        return values;
    }

    public ValidationInfo doValidate() {
        if (template == null || template.getOptions() == null) {
            return null;
        }
        List<ModelTemplate.Option> required = template.getOptions().stream()
                .filter(option -> BooleanUtils.isTrue(option.getRequired())).collect(Collectors.toList());
        Map<String, Object> optionValues = getOptionValues();
        for (ModelTemplate.Option option : required) {
            Object value = optionValues.get(option.getName());
            if (value == null || StringUtils.isEmpty(value.toString())) {
                return new ValidationInfo(option.getName() + " is required", optionsComponents.get(option.getName()));
            }
        }
        return null;
    }
}
