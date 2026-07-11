package io.apix.codegen.ui;

import com.github.jknack.handlebars.Handlebars;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import io.apix.codegen.core.GenerateConfigs;
import io.apix.codegen.generator.FileCodeGenerator;
import io.apix.codegen.model.CodeFile;
import io.apix.codegen.model.CodeTemplate;
import io.apix.document.Document;
import io.apix.model.Request;
import io.apix.util.FileWriteUtils;
import io.apix.util.HandlebarsUtils;
import io.apix.util.SwingUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateCodePanel extends JBPanel<GenerateCodePanel> {

    private final Project project;
    private JBList<String> languageList;
    private ToggleBar typesBar;
    private JPanel optionsPanel;
    private final FileCodeGenerator codeGenerator;
    private Map<String, JComponent> optionsComponents = new HashMap<>();
    private CodeTemplate template;
    private Document document;

    public GenerateCodePanel(Project project, Document document, List<Request> requests) {
        super(new BorderLayout());
        this.project = project;
        this.document = document;
        this.codeGenerator = new FileCodeGenerator(project, document, requests);

        setPreferredSize(new Dimension(880, 420));
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        JBSplitter splitter = new JBSplitter(false, "", 0.28f);
        splitter.setFirstComponent(leftPanel);
        splitter.setSecondComponent(rightPanel);
        add(splitter);

        languageList.addListSelectionListener(this::handleLanguageChange);
        typesBar.setChangeListener(this::handleRequestTypeChange);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setPreferredSize(new Dimension(270, 420));

        // 语言选择区域
        List<String> languages = GenerateConfigs.getInstance().getConfig().getCodeLanguages();
        JPanel languagePanel = new JPanel(new BorderLayout());
        languagePanel.setBorder(BorderFactory.createTitledBorder(""));
        languageList = new JBList<>(languages);

        // 设置自定义渲染器以显示语言图标
        languageList.setCellRenderer(new LanguageListCellRenderer());

        languagePanel.add(languageList, BorderLayout.CENTER);

        JScrollPane scrollPane = new JBScrollPane(languagePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        // 头部区域
        JPanel headerPanel = new JPanel(new BorderLayout());
        // 请求类型区域
        List<String> requestTypes = getRequestTypes();
        typesBar = new ToggleBar(requestTypes);
        typesBar.setVisible(CollectionUtils.isNotEmpty(requestTypes));
        headerPanel.add(typesBar, BorderLayout.WEST);

        // 编辑器区域
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridBagLayout());
        JPanel optionsWrapper = new JPanel(new BorderLayout());
        optionsWrapper.add(optionsPanel, BorderLayout.NORTH);

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(optionsWrapper, BorderLayout.CENTER);
        return rightPanel;
    }


    private List<String> getRequestTypes() {
        String language = languageList.getSelectedValue();
        if (StringUtils.isEmpty(language)) {
            return Collections.emptyList();
        }

        List<CodeTemplate> templates = GenerateConfigs.getInstance().getConfig().getCodeTemplates(language);
        return templates.stream().map(CodeTemplate::getName).collect(Collectors.toList());
    }

    public void setLanguage(String language) {
        languageList.setSelectedValue(language, true);
    }

    public void generate() {
        String language = languageList.getSelectedValue();
        if (StringUtils.isEmpty(language)) {
            return;
        }
        String type = typesBar.getSelectedText();
        if (StringUtils.isEmpty(type)) {
            return;
        }

        Map<String, Object> options = getOptionValues();
        List<CodeFile> files = codeGenerator.generate(language, type, options);
        files.forEach(file -> FileWriteUtils.write(project, file.getPath(), file.getCode()));
    }

    private void handleLanguageChange(ListSelectionEvent e) {
        List<String> requestTypes = getRequestTypes();
        typesBar.setVisible(CollectionUtils.isNotEmpty(requestTypes));
        typesBar.setOptions(requestTypes, true);
    }

    private void handleRequestTypeChange(String requestType) {
        optionsPanel.removeAll();
        optionsComponents.clear();
        String language = languageList.getSelectedValue();
        if (StringUtils.isEmpty(language)) {
            optionsPanel.revalidate();
            optionsPanel.repaint();
            return;
        }
        this.template = GenerateConfigs.getInstance().getConfig().getCodeTemplate(language, requestType);
        if (template == null) {
            optionsPanel.revalidate();
            optionsPanel.repaint();
            return;
        }

        List<CodeTemplate.Option> options = template.getOptions();
        if (CollectionUtils.isEmpty(options)) {
            optionsPanel.revalidate();
            optionsPanel.repaint();
            return;
        }
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        int row = 0;

        for (CodeTemplate.Option option : options) {
            if(BooleanUtils.isTrue(option.getHidden())) {
                continue;
            }
            String label = option.getLabel();
            if ("text".equals(option.getType())) {
                if (StringUtils.isNotEmpty(label)) {
                    gbc.gridx = 0;
                    gbc.gridy = row;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.weightx = 0;
                    optionsPanel.add(new JLabel(label), gbc);
                }
                gbc.gridx = 1;
                gbc.gridy = row;
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.HORIZONTAL; // 允许横向拉伸
                gbc.weightx = 1.0;
                String defaultValue = option.getDefaultValue() != null?option.getDefaultValue().toString():null;
                JTextField component = new JTextField(defaultValue);
                component.setPreferredSize(new Dimension(210, component.getPreferredSize().height));
                optionsComponents.put(option.getName(), component);
                optionsPanel.add(component, gbc);
            } else if ("list".equals(option.getType())) {
                if (StringUtils.isNotEmpty(label)) {
                    gbc.gridx = 0;
                    gbc.gridy = row;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.weightx = 0;
                    optionsPanel.add(new JLabel(label), gbc);
                }
                gbc.gridx = 1;
                gbc.gridy = row;
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.HORIZONTAL; // 允许横向拉伸
                gbc.weightx = 1.0;
                String defaultValue = option.getDefaultValue() != null?option.getDefaultValue().toString():null;
                List<String> values = option.getValues().stream().map(CodeTemplate.Value::getValue)
                        .collect(Collectors.toList());
                JComboBox<String> component = new ComboBox<>(values.toArray(new String[0]));
                component.setPreferredSize(new Dimension(210, component.getPreferredSize().height));
                component.setSelectedItem(defaultValue);
                optionsComponents.put(option.getName(), component);
                optionsPanel.add(component, gbc);
            } else if ("boolean".equals(option.getType())) {
                gbc.gridx = 1;
                gbc.gridy = row;
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.HORIZONTAL; // 允许横向拉伸
                gbc.weightx = 1.0;
                JBCheckBox component = new JBCheckBox(option.getLabel(), "true".equals(option.getDefaultValue()));
                component.setPreferredSize(new Dimension(210, component.getPreferredSize().height));
                optionsComponents.put(option.getName(), component);
                optionsPanel.add(component, gbc);
            }

            row++;
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private Map<String, Object> getOptionValues() {
        List<CodeTemplate.Option> options = template.getOptions();
        if(CollectionUtils.isEmpty(options)) {
            return Collections.emptyMap();
        }

        Map<String, Object> values = new HashMap<>();
        for (CodeTemplate.Option option : options) {
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
        List<CodeTemplate.Option> required = template.getOptions().stream().filter(option -> BooleanUtils.isTrue(option.getRequired())).collect(Collectors.toList());
        Map<String, Object> optionValues = getOptionValues();
        for (CodeTemplate.Option option : required) {
            if(BooleanUtils.isTrue(option.getHidden())) {
                continue;
            }
            Object value = optionValues.get(option.getName());
            if (value == null || (value instanceof String && StringUtils.isEmpty(value.toString()))) {
                return new ValidationInfo("Required value", optionsComponents.get(option.getName()));
            } else if(value instanceof String) {
                try {
                    Handlebars handlebars = HandlebarsUtils.create();
                    handlebars.compileInline((String) value);
                } catch (Exception e) {
                    return new ValidationInfo("Invalid handlebars syntax", optionsComponents.get(option.getName()));
                }
            }
        }
        return null;
    }


}
