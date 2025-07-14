package io.apicopilot.codegen.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import io.apicopilot.codegen.core.GenerateConfigs;
import io.apicopilot.codegen.generator.RequestCodeGenerator;
import io.apicopilot.codegen.model.RequestTemplate;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateRequestPanel extends JBPanel<GenerateRequestPanel> implements Disposable {

    private final Project project;
    private JBList<String> languageList;
    private ToggleBar requestTypesBar;
    private CodeEditorPanel codeEditor;
    private JButton copyButton;
    private final RequestCodeGenerator codeGenerator;

    public GenerateRequestPanel(Project project, Document document, Request request) {
        super(new BorderLayout());
        this.project = project;
        this.codeGenerator = new RequestCodeGenerator(document, request);

        setPreferredSize(new Dimension(880, 420));
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        JBSplitter splitter = new JBSplitter(false, "GenerateModelPanelSplitter", 0.28f);
        splitter.setFirstComponent(leftPanel);
        splitter.setSecondComponent(rightPanel);
        add(splitter);

        copyButton.addActionListener(this::handleCopy);
        languageList.addListSelectionListener(this::handleLanguageChange);
        requestTypesBar.setChangeListener(this::handleRequestTypeChange);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setPreferredSize(new Dimension(270, 420));

        // 语言选择区域
        List<String> languages = GenerateConfigs.getInstance().getConfig().getRequestLanguages();
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
        requestTypesBar = new ToggleBar(requestTypes);
        requestTypesBar.setVisible(CollectionUtils.isNotEmpty(requestTypes));
        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        copyButton = new DynamicButton("Copy", AllIcons.Actions.Copy, "Copied", AllIcons.Actions.Checked);
        buttonPanel.add(copyButton);

        headerPanel.add(requestTypesBar, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.CENTER);

        // 编辑器区域
        codeEditor = new CodeEditorPanel(this.project, false);

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(codeEditor, BorderLayout.CENTER);
        return rightPanel;
    }

    private List<String> getRequestTypes() {
        String language = languageList.getSelectedValue();
        if (StringUtils.isEmpty(language)) {
            return Collections.emptyList();
        }

        List<RequestTemplate> templates = GenerateConfigs.getInstance().getConfig().getRequestTemplates(language);
        return templates.stream().map(RequestTemplate::getName).collect(Collectors.toList());
    }

    public void setLanguage(String language) {
        languageList.setSelectedValue(language, true);
    }

    private void handleCopy(ActionEvent e) {
        String code = codeEditor.getText();
        StringSelection selection = new StringSelection(code);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void handleLanguageChange(ListSelectionEvent e) {
        List<String> requestTypes = getRequestTypes();
        requestTypesBar.setVisible(CollectionUtils.isNotEmpty(requestTypes));
        requestTypesBar.setOptions(requestTypes, true);
    }

    private void handleRequestTypeChange(String requestType) {
        String language = languageList.getSelectedValue();
        if (StringUtils.isEmpty(language)) {
            return;
        }
        if (requestType == null) {
            return;
        }

        String code = codeGenerator.generateCode(language, requestType);
        String extension = GenerateConfigs.getInstance().getConfig().getExtension(language);
        codeEditor.setText(code, extension);
    }

    @Override
    public void dispose() {
        codeEditor.dispose();
    }
}
