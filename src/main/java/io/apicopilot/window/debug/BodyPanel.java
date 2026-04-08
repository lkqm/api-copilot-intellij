package io.apicopilot.window.debug;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import io.apicopilot.codegen.ui.CodeEditorPanel;
import io.apicopilot.util.OpenApiUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Postman-style request body panel.
 *
 * <pre>
 * ◉ none  ○ form-data  ○ x-www-form-urlencoded  ○ raw  ○ binary    [JSON▼]
 * ┌───────────────────────────────────────────────────────────────┐
 * │  (none hint / form table / url-encoded table / code editor /  │
 * │   binary hint)                                                │
 * └───────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * The raw-type dropdown ([JSON▼]) is only visible when "raw" is selected.
 */
public class BodyPanel extends JPanel implements Disposable {

    // Raw format labels and matching file extensions / content-types
    private static final String RAW_JSON = "JSON";
    private static final String RAW_XML  = "XML";
    private static final String RAW_HTML = "HTML";
    private static final String RAW_TEXT = "Text";

    // Card keys
    private static final String CARD_NONE    = "none";
    private static final String CARD_FORM    = "form";
    private static final String CARD_URLENC  = "urlenc";
    private static final String CARD_RAW     = "raw";
    private static final String CARD_BINARY  = "binary";

    // UI controls
    private final JRadioButton btnNone       = radio("none");
    private final JRadioButton btnFormData   = radio("form-data");
    private final JRadioButton btnUrlEncoded = radio("x-www-form-urlencoded");
    private final JRadioButton btnRaw        = radio("raw");
    private final JRadioButton btnBinary     = radio("binary");
    private final JComboBox<String> rawTypeCombo = buildRawTypeCombo();

    // Content panels
    private final FormParamPanel  formDataPanel   = new FormParamPanel();
    private final FormParamPanel  urlEncodedPanel = new FormParamPanel();
    private final BinaryBodyPanel binaryPanel     = new BinaryBodyPanel();
    private final CodeEditorPanel rawEditor;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // multipart boundary (stable for the lifetime of this panel)
    private final String boundary = "----ApiCopilotBoundary" + UUID.randomUUID().toString().replace("-", "");

    // Raw action bar (auto-generate + beautify), visible only in raw mode
    private JPanel rawActionBar;

    // Current operation for schema-based auto-generation
    private Operation currentOperation;

    public BodyPanel(Project project) {
        super(new BorderLayout());
        rawEditor = new CodeEditorPanel(project, false);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCards(), BorderLayout.CENTER);

        selectNone();
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Pre-populate from the OpenAPI operation's requestBody. */
    public void setOperation(Operation operation) {
        this.currentOperation = operation;
        if (operation == null || operation.getRequestBody() == null) {
            selectNone();
            return;
        }
        Content content = operation.getRequestBody().getContent();
        if (content == null || content.isEmpty()) {
            selectNone();
            return;
        }
        if (content.containsKey("application/json")) {
            btnRaw.setSelected(true);
            rawTypeCombo.setSelectedItem(RAW_JSON);
            rawTypeCombo.setVisible(true);
            rawActionBar.setVisible(true);
            cardLayout.show(cardPanel, CARD_RAW);
            String example = extractExample(content.get("application/json"));
            rawEditor.setText(example != null ? example : "", "json");
        } else if (content.containsKey("application/xml")) {
            btnRaw.setSelected(true);
            rawTypeCombo.setSelectedItem(RAW_XML);
            rawTypeCombo.setVisible(true);
            rawActionBar.setVisible(true);
            cardLayout.show(cardPanel, CARD_RAW);
            rawEditor.setText("", "xml");
        } else if (content.containsKey("application/x-www-form-urlencoded")) {
            btnUrlEncoded.setSelected(true);
            rawTypeCombo.setVisible(false);
            populateFormPanel(urlEncodedPanel, content.get("application/x-www-form-urlencoded"));
            cardLayout.show(cardPanel, CARD_URLENC);
        } else if (content.containsKey("multipart/form-data")) {
            btnFormData.setSelected(true);
            rawTypeCombo.setVisible(false);
            populateFormPanel(formDataPanel, content.get("multipart/form-data"));
            cardLayout.show(cardPanel, CARD_FORM);
        } else if (content.containsKey("application/octet-stream")) {
            selectBinary();
        } else if (content.entrySet().stream().anyMatch(entry -> isBinaryMediaType(entry.getKey(), entry.getValue()))) {
            selectBinary();
        } else {
            selectNone();
        }
    }

    /** Returns the serialized text body, or null when type is none/binary. */
    public String getBody() {
        if (btnNone.isSelected() || btnBinary.isSelected()) return null;
        if (btnFormData.isSelected())   return buildMultipartBody(formDataPanel.getParams());
        if (btnUrlEncoded.isSelected()) return buildUrlEncodedBody(urlEncodedPanel.getParams());
        // raw
        String text = rawEditor.getText().trim();
        return text.isEmpty() ? null : rawEditor.getText();
    }

    /** Returns binary file bytes, or null when binary mode is not active / no file selected. */
    public byte[] getBinaryBody() {
        return btnBinary.isSelected() ? binaryPanel.getFileBytes() : null;
    }

    /**
     * Returns multipart/form-data body as bytes when the form contains file-type fields,
     * otherwise null (use {@link #getBody()} for text-only form-data).
     */
    public byte[] getFormDataBodyBytes() {
        if (!btnFormData.isSelected()) return null;
        List<FormParam> params = formDataPanel.getParams();
        boolean hasFileField = params.stream().anyMatch(p -> p.isFile);
        if (!hasFileField) return null;
        return buildMultipartBodyBytes(params);
    }

    /** Returns true when the body type is anything other than none. */
    public boolean hasValues() {
        return !btnNone.isSelected();
    }

    public void addValueChangeListener(Runnable listener) {
        for (JRadioButton btn : new JRadioButton[]{btnNone, btnFormData, btnUrlEncoded, btnRaw, btnBinary}) {
            btn.addActionListener(e -> listener.run());
        }
    }

    /** Wire up a listener that receives the detected Content-Type when a binary file is selected. */
    public void setBinaryContentTypeListener(java.util.function.Consumer<String> listener) {
        binaryPanel.setOnContentTypeDetected(listener);
    }

    /** Returns the Content-Type header value, or null when none/binary (binary CT lives in Headers). */
    public String getContentType() {
        if (btnNone.isSelected()) return null;
        if (btnBinary.isSelected())     return null; // pushed directly to Headers panel
        if (btnFormData.isSelected())   return "multipart/form-data; boundary=" + boundary;
        if (btnUrlEncoded.isSelected()) return "application/x-www-form-urlencoded";
        // raw
        Object sel = rawTypeCombo.getSelectedItem();
        if (RAW_JSON.equals(sel)) return "application/json";
        if (RAW_XML.equals(sel))  return "application/xml";
        if (RAW_HTML.equals(sel)) return "text/html";
        return "text/plain";
    }

    @Override
    public void dispose() {
        rawEditor.dispose();
    }

    // ── Build helpers ─────────────────────────────────────────────────────

    private static JComboBox<String> buildRawTypeCombo() {
        JComboBox<String> combo = new JComboBox<>(new String[]{RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT});

        // Flat look: no border, no background — just "Text ▾"
        combo.setOpaque(false);
        combo.setBorder(BorderFactory.createEmptyBorder());
        combo.setFocusable(false);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (index == -1) {
                    // Selected item shown in the combo "button" area
                    label.setText(value + " \u25BE"); // ▾
                    label.setOpaque(false);
                    label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                    label.setFont(label.getFont().deriveFont(12f));
                }
                return label;
            }
        });

        // Replace the UI so the button area is invisible
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                btn.setMinimumSize(new Dimension(0, 0));
                btn.setMaximumSize(new Dimension(0, 0));
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(false);
                btn.setFocusable(false);
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // no background
            }
        });

        return combo;
    }

    private JPanel buildToolbar() {
        // Radio button group
        ButtonGroup group = new ButtonGroup();
        for (JRadioButton btn : new JRadioButton[]{btnNone, btnFormData, btnUrlEncoded, btnRaw, btnBinary}) {
            group.add(btn);
        }

        rawActionBar = buildRawActionBar();

        btnNone.addActionListener(e -> selectNone());
        btnFormData.addActionListener(e -> { rawTypeCombo.setVisible(false); rawActionBar.setVisible(false); cardLayout.show(cardPanel, CARD_FORM); });
        btnUrlEncoded.addActionListener(e -> { rawTypeCombo.setVisible(false); rawActionBar.setVisible(false); cardLayout.show(cardPanel, CARD_URLENC); });
        btnRaw.addActionListener(e -> { rawTypeCombo.setVisible(true); rawActionBar.setVisible(true); cardLayout.show(cardPanel, CARD_RAW); onRawTypeChanged(); });
        btnBinary.addActionListener(e -> { rawTypeCombo.setVisible(false); rawActionBar.setVisible(false); cardLayout.show(cardPanel, CARD_BINARY); });

        rawTypeCombo.addActionListener(e -> onRawTypeChanged());
        rawTypeCombo.setVisible(false);

        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        firstRow.add(btnNone);
        firstRow.add(btnFormData);
        firstRow.add(btnUrlEncoded);
        firstRow.add(btnRaw);
        firstRow.add(btnBinary);
        firstRow.add(rawTypeCombo);

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setBorder(JBUI.Borders.customLine(UIManager.getColor("Separator.separatorColor"), 0, 0, 1, 0));
        toolbar.add(firstRow);
        toolbar.add(rawActionBar);
        return toolbar;
    }

    private JPanel buildRawActionBar() {
        JButton autoGenBtn = buildTextButton("Auto Generate", new Color(0x2E7D32));
        autoGenBtn.addActionListener(e -> onAutoGenerate());

        JButton beautifyBtn = buildTextButton("Beautify", new Color(0x1565C0));
        beautifyBtn.addActionListener(e -> onBeautify());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        bar.add(autoGenBtn);
        bar.add(beautifyBtn);
        bar.setVisible(false);
        return bar;
    }

    /** A flat text-only button: colored label, no border/background; hover shows subtle bg. */
    private static JButton buildTextButton(String text, Color textColor) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    g.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 18));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }
                super.paintComponent(g);
            }
        };
        btn.setForeground(textColor);
        btn.setFont(btn.getFont().deriveFont(12f));
        btn.setFocusable(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(1, 2, 1, 2));
        return btn;
    }

    private JPanel buildCards() {
        // none card
        JBLabel noneHint = new JBLabel("This request does not have a body.");
        noneHint.setHorizontalAlignment(SwingConstants.CENTER);
        noneHint.setForeground(UIManager.getColor("Label.disabledForeground"));

        cardPanel.add(noneHint,       CARD_NONE);
        cardPanel.add(formDataPanel,  CARD_FORM);
        cardPanel.add(urlEncodedPanel,CARD_URLENC);
        cardPanel.add(rawEditor,      CARD_RAW);
        cardPanel.add(binaryPanel,    CARD_BINARY);
        return cardPanel;
    }

    private void selectNone() {
        btnNone.setSelected(true);
        rawTypeCombo.setVisible(false);
        if (rawActionBar != null) rawActionBar.setVisible(false);
        cardLayout.show(cardPanel, CARD_NONE);
    }

    private void selectBinary() {
        btnBinary.setSelected(true);
        rawTypeCombo.setVisible(false);
        if (rawActionBar != null) rawActionBar.setVisible(false);
        cardLayout.show(cardPanel, CARD_BINARY);
    }

    private void onRawTypeChanged() {
        Object sel = rawTypeCombo.getSelectedItem();
        String ext = RAW_JSON.equals(sel) ? "json"
                   : RAW_XML.equals(sel)  ? "xml"
                   : RAW_HTML.equals(sel) ? "html"
                   : "txt";
        rawEditor.setText(rawEditor.getText(), ext);
    }

    // ── Body serialization ────────────────────────────────────────────────

    private String buildUrlEncodedBody(List<FormParam> params) {
        StringBuilder sb = new StringBuilder();
        for (FormParam p : params) {
            if (!p.enabled || p.name.isEmpty()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(p.name)).append('=').append(enc(p.value));
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String buildMultipartBody(List<FormParam> params) {
        StringBuilder sb = new StringBuilder();
        for (FormParam p : params) {
            if (!p.enabled || p.name.isEmpty() || p.isFile) continue;
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"").append(p.name).append("\"\r\n");
            sb.append("\r\n");
            sb.append(p.value).append("\r\n");
        }
        if (sb.length() == 0) return null;
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString();
    }

    private byte[] buildMultipartBodyBytes(List<FormParam> params) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] crlf   = "\r\n".getBytes(StandardCharsets.UTF_8);
            String bStart = "--" + boundary;

            for (FormParam p : params) {
                if (!p.enabled || p.name.isEmpty()) continue;
                if (p.isFile) {
                    for (File file : p.files) {
                        String ct = guessContentType(file.getName());
                        out.write((bStart + "\r\n").getBytes(StandardCharsets.UTF_8));
                        out.write(("Content-Disposition: form-data; name=\"" + p.name
                                + "\"; filename=\"" + file.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                        out.write(("Content-Type: " + ct + "\r\n").getBytes(StandardCharsets.UTF_8));
                        out.write(crlf);
                        out.write(Files.readAllBytes(file.toPath()));
                        out.write(crlf);
                    }
                } else {
                    out.write((bStart + "\r\n").getBytes(StandardCharsets.UTF_8));
                    out.write(("Content-Disposition: form-data; name=\"" + p.name + "\"\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    out.write(crlf);
                    out.write(p.value.getBytes(StandardCharsets.UTF_8));
                    out.write(crlf);
                }
            }
            out.write((bStart + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml"))  return "application/xml";
        if (lower.endsWith(".txt"))  return "text/plain";
        if (lower.endsWith(".zip"))  return "application/zip";
        return "application/octet-stream";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }

    // ── Raw actions ───────────────────────────────────────────────────────

    private void onAutoGenerate() {
        Object sel = rawTypeCombo.getSelectedItem();
        if (!RAW_JSON.equals(sel) && !RAW_XML.equals(sel)) return;

        String contentType = RAW_JSON.equals(sel) ? "application/json" : "application/xml";
        Schema<?> schema = extractRequestBodySchema(currentOperation, contentType);

        if (RAW_JSON.equals(sel)) {
            rawEditor.setText(generateMockJson(schema), "json");
        } else {
            rawEditor.setText(generateMockXml(schema), "xml");
        }
    }

    private void onBeautify() {
        String text = rawEditor.getText().trim();
        if (text.isEmpty()) return;

        Object sel = rawTypeCombo.getSelectedItem();
        if (RAW_JSON.equals(sel)) {
            try {
                JsonElement element = JsonParser.parseString(text);
                String pretty = new GsonBuilder().setPrettyPrinting().create().toJson(element);
                rawEditor.setText(pretty, "json");
            } catch (Exception ignored) { }
        } else if (RAW_XML.equals(sel)) {
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                StringWriter sw = new StringWriter();
                transformer.transform(new StreamSource(new StringReader(text)), new StreamResult(sw));
                rawEditor.setText(sw.toString().trim(), "xml");
            } catch (Exception ignored) { }
        }
    }

    private String generateMockJson(Schema<?> schema) {
        Object mockValue = schema != null ? generateMockValue(schema) : new LinkedHashMap<>();
        return new GsonBuilder().setPrettyPrinting().create().toJson(mockValue);
    }

    @SuppressWarnings("unchecked")
    private String generateMockXml(Schema<?> schema) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        Map<String, Schema> props = schema != null ? schema.getProperties() : null;
        if (props == null || props.isEmpty()) {
            sb.append("<root/>");
        } else {
            sb.append("<root>\n");
            props.forEach((name, prop) -> {
                Object val = generateMockValue(prop);
                sb.append("  <").append(name).append(">")
                  .append(val != null ? val : "")
                  .append("</").append(name).append(">\n");
            });
            sb.append("</root>");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Object generateMockValue(Schema<?> schema) {
        if (schema == null) return null;

        // Prefer example > default > first enum
        if (schema.getExample() != null) return schema.getExample();
        if (schema.getDefault() != null) return schema.getDefault();
        List<?> enums = schema.getEnum();
        if (enums != null && !enums.isEmpty()) return enums.get(0);

        String type   = schema.getType();
        String format = schema.getFormat();

        // Object
        if ("object".equals(type) || (type == null && schema.getProperties() != null)) {
            LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
            Map<String, Schema> props = schema.getProperties();
            if (props != null) props.forEach((name, prop) -> obj.put(name, generateMockValue(prop)));
            return obj;
        }

        // Array
        if ("array".equals(type)) {
            List<Object> list = new ArrayList<>();
            Schema<?> items = schema.getItems();
            if (items != null) list.add(generateMockValue(items));
            return list;
        }

        if ("integer".equals(type)) return 1;
        if ("number".equals(type))  return 1.0;
        if ("boolean".equals(type)) return true;

        // String formats
        if ("email".equals(format))               return "user@example.com";
        if ("uuid".equals(format))                return UUID.randomUUID().toString();
        if ("date".equals(format))                return "2025-01-01";
        if ("date-time".equals(format))           return "2025-01-01T00:00:00Z";
        if ("uri".equals(format) || "url".equals(format)) return "https://example.com";
        if ("password".equals(format))            return "password123";

        return "string";
    }

    private static Schema<?> extractRequestBodySchema(Operation operation, String contentType) {
        if (operation == null) return null;
        RequestBody rb = operation.getRequestBody();
        if (rb == null || rb.getContent() == null) return null;
        MediaType mt = rb.getContent().get(contentType);
        return mt != null ? mt.getSchema() : null;
    }

    private static boolean isBinaryMediaType(String contentType, MediaType mediaType) {
        if (contentType == null) return false;
        if ("application/octet-stream".equalsIgnoreCase(contentType)) return true;

        Schema<?> schema = mediaType != null ? mediaType.getSchema() : null;
        if (schema == null) return false;

        String type = schema.getType();
        String format = schema.getFormat();
        return "string".equals(type) && ("binary".equals(format) || "base64".equals(format));
    }

    // ── OpenAPI helpers ───────────────────────────────────────────────────

    private static String extractExample(MediaType mediaType) {
        if (mediaType == null) return null;
        Object ex = mediaType.getExample();
        return ex != null ? ex.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static void populateFormPanel(FormParamPanel panel, MediaType mediaType) {
        panel.clearRows();
        if (mediaType != null) {
            Schema<?> schema = mediaType.getSchema();
            if (schema != null && schema.getProperties() != null) {
                schema.getProperties().forEach((name, prop) -> {
                    String type   = OpenApiUtils.schemaTypeDisplay((Schema<?>) prop);
                    String desc   = ((Schema<?>) prop).getDescription() != null ? ((Schema<?>) prop).getDescription() : "";
                    String format = ((Schema<?>) prop).getFormat();
                    boolean isFile = "binary".equals(format) || "base64".equals(format);
                    if (isFile) {
                        panel.addFileRow(true, (String) name, type, desc);
                    } else {
                        panel.addRow(true, (String) name, "", type, desc);
                    }
                });
            }
        }
        panel.ensureGhostRow();
    }

    private static JRadioButton radio(String text) {
        JRadioButton btn = new JRadioButton(text);
        btn.setFont(btn.getFont().deriveFont(12f));
        btn.setFocusable(false);
        return btn;
    }

    // ── Inner: FormParamPanel ─────────────────────────────────────────────

    /**
     * Editable key-value table for form-data and x-www-form-urlencoded params, with bulk-edit support.
     * Columns: [☑] Name | Value | Type | Description | ×
     *
     * Postman-style: ghost row at bottom, inline × delete, no +/- toolbar.
     */
    static class FormParamPanel extends BulkEditablePanel {

        private static final int COL_ENABLED = 0;
        private static final int COL_NAME    = 1;
        private static final int COL_VALUE   = 2;
        private static final int COL_TYPE    = 3;
        private static final int COL_DESC    = 4;
        private static final int COL_DELETE  = 5;

        private static final int BADGE_W = JBUI.scale(28); // type badge width in Name column

        private final DefaultTableModel model;
        private final JBTable table;
        private boolean suppressGhostAdd = false;
        private int hoveredNameRow = -1; // row whose Name cell is currently hovered

        /** Sentinel stored in COL_VALUE for file-type parameters (format: binary). */
        static class FileValue {
            final List<File> files;
            FileValue() { this.files = new ArrayList<>(); }
            FileValue(List<File> files) { this.files = new ArrayList<>(files); }
        }

        FormParamPanel() {
            model = new DefaultTableModel(new Object[]{"", "Name", "Value", "Type", "Description", ""}, 0) {
                @Override public Class<?> getColumnClass(int col) {
                    if (col == COL_ENABLED) return Boolean.class;
                    if (col == COL_VALUE)   return Object.class;
                    return String.class;
                }
                @Override public boolean isCellEditable(int row, int col) {
                    if (col == COL_DELETE) return false;
                    if (col == COL_DESC) return false;
                    if (col == COL_ENABLED && isGhostRow(row)) return false;
                    if (col == COL_VALUE && model.getValueAt(row, COL_VALUE) instanceof FileValue) return false;
                    return true;
                }
            };

            model.addTableModelListener(e -> {
                if (suppressGhostAdd || (e.getColumn() != COL_NAME && e.getColumn() != COL_VALUE)) return;
                int lastRow = model.getRowCount() - 1;
                if (lastRow >= 0) {
                    String name = (String) model.getValueAt(lastRow, COL_NAME);
                    Object value = model.getValueAt(lastRow, COL_VALUE);
                    if ((name != null && !name.isEmpty()) || hasValueContent(value)) {
                        if (!Boolean.TRUE.equals(model.getValueAt(lastRow, COL_ENABLED))) {
                            model.setValueAt(Boolean.TRUE, lastRow, COL_ENABLED);
                        }
                        suppressGhostAdd = true;
                        model.addRow(new Object[]{false, "", "", "string", "", null});
                        suppressGhostAdd = false;
                    }
                }
            });

            table = new JBTable(model) {
                @Override
                public boolean editCellAt(int row, int column, java.util.EventObject e) {
                    if (e instanceof MouseEvent) {
                        int modelCol = convertColumnIndexToModel(column);
                        if (modelCol == COL_NAME && !isGhostRow(row)) {
                            Rectangle cellRect = getCellRect(row, column, false);
                            if (isOnTypeBadge((MouseEvent) e, cellRect)) {
                                return false;
                            }
                        }
                    }
                    return super.editCellAt(row, column, e);
                }
            };
            table.setRowHeight(JBUI.scale(24));
            JTextField editorField = new JTextField();
            editorField.setBorder(JBUI.Borders.empty(0, 4));
            DefaultCellEditor singleClickEditor = new DefaultCellEditor(editorField);
            singleClickEditor.setClickCountToStart(1);
            TableEditorNavigation.install(table);
            table.getColumnModel().getColumn(COL_ENABLED).setMaxWidth(JBUI.scale(30));
            table.getColumnModel().getColumn(COL_ENABLED).setMinWidth(JBUI.scale(30));
            table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(JBUI.scale(110));
            table.getColumnModel().getColumn(COL_VALUE).setPreferredWidth(JBUI.scale(200));
            table.getColumnModel().getColumn(COL_DESC).setPreferredWidth(JBUI.scale(160));
            table.getColumnModel().getColumn(COL_DELETE).setMaxWidth(JBUI.scale(28));
            table.getColumnModel().getColumn(COL_DELETE).setMinWidth(JBUI.scale(28));
            table.getTableHeader().setReorderingAllowed(false);
            table.getColumnModel().getColumn(COL_DELETE).setCellRenderer(new DeleteButtonRenderer());
            table.getColumnModel().getColumn(COL_NAME).setCellEditor(singleClickEditor);
            table.getColumnModel().getColumn(COL_VALUE).setCellEditor(singleClickEditor);
            table.removeColumn(table.getColumnModel().getColumn(COL_TYPE));

            table.getColumnModel().getColumn(COL_NAME).setCellRenderer(new NameCellRenderer());
            table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean isSelected, boolean hasFocus, int row, int col) {
                    return super.getTableCellRendererComponent(t, value, false, false, row, col);
                }
            });
            table.setDefaultRenderer(Boolean.class, new TableCellRenderer() {
                private final JCheckBox cb = new JCheckBox();
                { cb.setHorizontalAlignment(SwingConstants.CENTER); cb.setOpaque(true); cb.setFocusable(false); }
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean isSelected, boolean hasFocus, int row, int col) {
                    cb.setSelected(Boolean.TRUE.equals(value));
                    cb.setBackground(t.getBackground());
                    cb.setEnabled(!isGhostRow(row));
                    return cb;
                }
            });
            // Value column: render FileValue with filename/count + clear button next to text
            table.getColumnModel().getColumn(COL_VALUE).setCellRenderer(new TableCellRenderer() {
                // outer panel fills the cell; infoPanel holds text + × side by side at left
                private final JPanel  outer      = new JPanel(new BorderLayout());
                private final JPanel  infoPanel  = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                private final JLabel  textLabel  = new JLabel();
                private final JLabel  clearBtn   = new JLabel("×");
                private final JLabel  plainLabel = new JLabel();
                {
                    textLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
                    clearBtn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                    clearBtn.setFont(clearBtn.getFont().deriveFont(12f));
                    infoPanel.add(textLabel);
                    infoPanel.add(clearBtn);
                    infoPanel.setOpaque(true);
                    outer.add(infoPanel, BorderLayout.WEST);
                    outer.setOpaque(true);
                }
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean isSelected, boolean hasFocus, int row, int col) {
                    if (value instanceof FileValue) {
                        FileValue fv = (FileValue) value;
                        Color bg = t.getBackground();
                        outer.setBackground(bg);
                        infoPanel.setBackground(bg);
                        textLabel.setBackground(bg);
                        textLabel.setOpaque(true);
                        textLabel.setFont(t.getFont());
                        if (fv.files.isEmpty()) {
                            textLabel.setText("Select files");
                            textLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                            clearBtn.setVisible(false);
                        } else if (fv.files.size() == 1) {
                            textLabel.setText(fv.files.get(0).getName());
                            textLabel.setForeground(UIManager.getColor("Label.foreground"));
                            clearBtn.setVisible(true);
                            clearBtn.setForeground(UIManager.getColor("Label.foreground"));
                        } else {
                            textLabel.setText(fv.files.size() + " files selected");
                            textLabel.setForeground(UIManager.getColor("Label.foreground"));
                            clearBtn.setVisible(true);
                            clearBtn.setForeground(UIManager.getColor("Label.foreground"));
                        }
                        return outer;
                    }
                    // Plain string — show type as placeholder when value is empty
                    String strVal = value instanceof String ? (String) value : "";
                    if (!isGhostRow(row) && strVal.isEmpty()) {
                        String type = (String) model.getValueAt(row, COL_TYPE);
                        if (type != null && !type.isEmpty()) {
                            plainLabel.setText(type);
                            plainLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                            plainLabel.setOpaque(true);
                            plainLabel.setBackground(t.getBackground());
                            plainLabel.setFont(t.getFont());
                            plainLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
                            return plainLabel;
                        }
                    }
                    plainLabel.setText(strVal);
                    plainLabel.setOpaque(true);
                    plainLabel.setBackground(t.getBackground());
                    plainLabel.setForeground(t.getForeground());
                    plainLabel.setFont(t.getFont());
                    plainLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
                    return plainLabel;
                }
            });

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = table.columnAtPoint(e.getPoint());
                    int row = table.rowAtPoint(e.getPoint());
                    if (col < 0 || row < 0) return;
                    int modelCol = table.convertColumnIndexToModel(col);
                    Rectangle cellRect = table.getCellRect(row, col, false);

                    if (modelCol == COL_NAME && !isGhostRow(row) && isOnTypeBadge(e, cellRect)) {
                        // Type badge takes precedence over starting text edit in Name cell.
                        showTypeSwitchPopup(row, e.getComponent(), e.getX(), e.getY());
                        return;
                    }

                    if (modelCol == COL_DELETE && !isGhostRow(row)) {
                        if (table.isEditing()) table.getCellEditor().stopCellEditing();
                        suppressGhostAdd = true;
                        model.removeRow(row);
                        suppressGhostAdd = false;
                        ensureGhostRow();
                        return;
                    }

                    if (modelCol == COL_VALUE
                            && model.getValueAt(row, COL_VALUE) instanceof FileValue) {
                        if (isOnClearBtn(e, row, cellRect)) {
                            model.setValueAt(new FileValue(), row, COL_VALUE);
                        } else {
                            openFileChooser(row);
                        }
                        return;
                    }

                    startEditingCell(row, col, e);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    table.setCursor(Cursor.getDefaultCursor());
                    if (hoveredNameRow >= 0) {
                        int old = hoveredNameRow;
                        hoveredNameRow = -1;
                        table.repaint(table.getCellRect(old, table.convertColumnIndexToView(COL_NAME), false));
                    }
                }
            });
            table.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int col = table.columnAtPoint(e.getPoint());
                    int row = table.rowAtPoint(e.getPoint());
                    int modelCol = col >= 0 ? table.convertColumnIndexToModel(col) : -1;

                    // Track hovered row for Name column badge
                    int newHoveredName = (modelCol == COL_NAME && row >= 0 && !isGhostRow(row)) ? row : -1;
                    if (newHoveredName != hoveredNameRow) {
                        int old = hoveredNameRow;
                        hoveredNameRow = newHoveredName;
                        int viewNameCol = table.convertColumnIndexToView(COL_NAME);
                        if (old >= 0) table.repaint(table.getCellRect(old, viewNameCol, false));
                        if (hoveredNameRow >= 0) table.repaint(table.getCellRect(hoveredNameRow, viewNameCol, false));
                    }

                    // Cursor
                    if (modelCol == COL_VALUE && row >= 0) {
                        Rectangle cellRect = table.getCellRect(row, col, false);
                        if (isOnClearBtn(e, row, cellRect)) {
                            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                    if (modelCol == COL_NAME && row >= 0 && !isGhostRow(row)) {
                        Rectangle nr = table.getCellRect(row, col, false);
                        if (isOnTypeBadge(e, nr)) {
                            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                    table.setCursor(Cursor.getDefaultCursor());
                }
            });

            ensureGhostRow();
            JBScrollPane scrollPane = new JBScrollPane(table);
            scrollPane.setBorder(JBUI.Borders.empty());
            scrollPane.setFocusable(false);
            initTableCard(scrollPane);
        }

        /** Returns true when the mouse event lands on the × clear button of a file row. */
        private boolean isOnClearBtn(MouseEvent e, int row, Rectangle cellRect) {
            Object val = model.getValueAt(row, COL_VALUE);
            if (!(val instanceof FileValue)) return false;
            FileValue fv = (FileValue) val;
            if (fv.files.isEmpty()) return false;
            String text = fv.files.size() == 1
                    ? fv.files.get(0).getName()
                    : fv.files.size() + " files selected";
            FontMetrics fm = table.getFontMetrics(table.getFont());
            // textLabel: 2px left border + text; clearBtn: 4px left pad + "×" + 4px right pad
            int clearStart = cellRect.x + 2 + fm.stringWidth(text) + 4;
            int clearEnd   = clearStart + fm.stringWidth("×") + 4;
            return e.getX() >= clearStart && e.getX() < clearEnd;
        }

        private boolean isOnTypeBadge(MouseEvent e, Rectangle cellRect) {
            return e.getX() >= cellRect.x + cellRect.width - BADGE_W;
        }

        private void startEditingCell(int row, int viewCol, MouseEvent e) {
            int modelCol = table.convertColumnIndexToModel(viewCol);
            if (!table.isCellEditable(row, viewCol)) return;
            if (modelCol != COL_NAME && modelCol != COL_VALUE) return;

            table.changeSelection(row, viewCol, false, false);
            if (!table.editCellAt(row, viewCol, e)) return;

            Component editor = table.getEditorComponent();
            if (editor != null) {
                editor.requestFocusInWindow();
                if (editor instanceof JTextField) {
                    ((JTextField) editor).selectAll();
                }
            }
        }

        void addRow(boolean enabled, String name, String value, String type, String desc) {
            model.addRow(new Object[]{enabled, name, value, type, desc, null});
        }

        void addFileRow(boolean enabled, String name, String type, String desc) {
            model.addRow(new Object[]{enabled, name, new FileValue(), type, desc, null});
        }

        void clearRows() {
            suppressGhostAdd = true;
            model.setRowCount(0);
            suppressGhostAdd = false;
        }

        void ensureGhostRow() {
            if (model.getRowCount() == 0 || !isGhostRow(model.getRowCount() - 1)) {
                suppressGhostAdd = true;
                model.addRow(new Object[]{false, "", "", "string", "", null});
                suppressGhostAdd = false;
            }
        }

        // ── Type switch popup ──────────────────────────────────────────────

        private void showTypeSwitchPopup(int row, Component invoker, int x, int y) {
            boolean isFile = model.getValueAt(row, COL_VALUE) instanceof FileValue;

            JPopupMenu menu = new JPopupMenu();

            JMenuItem textItem = new JMenuItem((isFile ? "    " : "\u2713 ") + "Text");
            textItem.addActionListener(e -> {
                if (isFile) {
                    model.setValueAt("", row, COL_VALUE);
                    model.setValueAt("string", row, COL_TYPE);
                }
            });
            menu.add(textItem);

            JMenuItem fileItem = new JMenuItem((isFile ? "\u2713 " : "    ") + "File");
            fileItem.addActionListener(e -> {
                if (!isFile) {
                    model.setValueAt(new FileValue(), row, COL_VALUE);
                    model.setValueAt("file", row, COL_TYPE);
                    // Ensure the row is enabled
                    if (!Boolean.TRUE.equals(model.getValueAt(row, COL_ENABLED))) {
                        model.setValueAt(Boolean.TRUE, row, COL_ENABLED);
                    }
                }
            });
            menu.add(fileItem);

            menu.show(invoker, x, y);
        }

        private void openFileChooser(int row) {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                    .createMultipleFilesNoJarsDescriptor()
                    .withTitle("Select Files");
            VirtualFile[] vFiles = FileChooser.chooseFiles(descriptor, null, null);
            if (vFiles.length > 0) {
                List<File> files = new ArrayList<>();
                for (VirtualFile vf : vFiles) files.add(VfsUtil.virtualToIoFile(vf));
                model.setValueAt(new FileValue(files), row, COL_VALUE);
            }
        }

        // ── Name column renderer ───────────────────────────────────────────

        private class NameCellRenderer implements TableCellRenderer {
            private final JPanel  panel     = new JPanel(null); // absolute layout
            private final JLabel  badge     = new JLabel();
            private final JLabel  nameLabel = new JLabel();
            private boolean       badgeHovered = false;

            NameCellRenderer() {
                panel.setOpaque(true);
                badge.setFont(badge.getFont().deriveFont(Font.BOLD, JBUI.scaleFontSize(9f)));
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
                panel.add(badge);
                panel.add(nameLabel);
            }

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                String text = value instanceof String ? (String) value : "";
                boolean hovered = (row == hoveredNameRow);
                boolean isFile  = model.getValueAt(row, COL_VALUE) instanceof FileValue;

                panel.setBackground(t.getBackground());

                int colW = t.getColumnModel().getColumn(col).getWidth();
                int h    = t.getRowHeight(row);

                if (hovered && !isGhostRow(row)) {
                    // Badge at the right end of the cell
                    badge.setVisible(true);
                    badge.setText(isFile ? "\u2261\u25BE" : "T\u25BE"); // ≡▾ or T▾
                    Color badgeColor = isFile
                            ? UIManager.getColor("Label.infoForeground")
                            : UIManager.getColor("Label.disabledForeground");
                    if (badgeColor == null) badgeColor = UIManager.getColor("Label.disabledForeground");
                    badge.setForeground(badgeColor);
                    badge.setOpaque(true);
                    Color bgColor = UIManager.getColor("ActionButton.hoverBackground");
                    badge.setBackground(bgColor != null ? bgColor : panel.getBackground());

                    nameLabel.setBounds(2, 0, colW - BADGE_W - 2, h);
                    badge.setBounds(colW - BADGE_W, 0, BADGE_W, h);
                } else {
                    badge.setVisible(false);
                    nameLabel.setBounds(2, 0, colW - 2, h);
                }

                nameLabel.setText(text);
                nameLabel.setForeground(t.getForeground());
                nameLabel.setFont(t.getFont());
                nameLabel.setOpaque(false);

                return panel;
            }
        }

        List<FormParam> getParams() {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            List<FormParam> result = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                if (isGhostRow(i)) continue;
                boolean enabled = Boolean.TRUE.equals(model.getValueAt(i, COL_ENABLED));
                String  name    = (String) model.getValueAt(i, COL_NAME);
                Object  val     = model.getValueAt(i, COL_VALUE);
                if (val instanceof FileValue) {
                    result.add(new FormParam(enabled, name != null ? name : "", ((FileValue) val).files));
                } else {
                    String value = val instanceof String ? (String) val : "";
                    result.add(new FormParam(enabled, name != null ? name : "", value));
                }
            }
            return result;
        }

        @Override
        protected String toBulkText() {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < model.getRowCount(); i++) {
                if (isGhostRow(i)) continue;
                Object val = model.getValueAt(i, COL_VALUE);
                if (val instanceof FileValue) continue; // file rows not editable in bulk mode
                boolean enabled = Boolean.TRUE.equals(model.getValueAt(i, COL_ENABLED));
                String  name    = (String) model.getValueAt(i, COL_NAME);
                sb.append(toBulkLine(enabled, name, val instanceof String ? (String) val : "")).append('\n');
            }
            return sb.toString().stripTrailing();
        }

        @Override
        protected void fromBulkText(String text) {
            suppressGhostAdd = true;
            model.setRowCount(0);
            for (String line : text.split("\n", -1)) {
                String[] parsed = parseBulkLine(line);
                if (parsed == null) continue;
                model.addRow(new Object[]{Boolean.parseBoolean(parsed[2]), parsed[0], parsed[1], "string", "", null});
            }
            suppressGhostAdd = false;
            ensureGhostRow();
        }

        private boolean isGhostRow(int row) {
            return row == model.getRowCount() - 1
                    && isBlank((String) model.getValueAt(row, COL_NAME))
                    && !hasValueContent(model.getValueAt(row, COL_VALUE));
        }

        private boolean hasValueContent(Object value) {
            if (value instanceof FileValue) {
                return !((FileValue) value).files.isEmpty();
            }
            return value instanceof String && !((String) value).isEmpty();
        }

        private boolean isBlank(String value) {
            return value == null || value.isEmpty();
        }

        private class DeleteButtonRenderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        t, value, false, false, row, col);
                label.setHorizontalAlignment(JLabel.CENTER);
                if (isGhostRow(row)) {
                    label.setText("");
                } else {
                    label.setText("×");
                    Color fg = UIManager.getColor("Label.foreground");
                    label.setForeground(fg != null ? fg : Color.DARK_GRAY);
                }
                return label;
            }
        }
    }

    // ── Inner: FormParam ──────────────────────────────────────────────────

    static class FormParam {
        final boolean    enabled;
        final String     name;
        final String     value;
        final boolean    isFile;
        final List<File> files;

        FormParam(boolean enabled, String name, String value) {
            this.enabled = enabled;
            this.name    = name;
            this.value   = value;
            this.isFile  = false;
            this.files   = Collections.emptyList();
        }

        FormParam(boolean enabled, String name, List<File> files) {
            this.enabled = enabled;
            this.name    = name;
            this.value   = "";
            this.isFile  = true;
            this.files   = files != null ? files : Collections.emptyList();
        }
    }
}
