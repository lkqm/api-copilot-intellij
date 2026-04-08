package io.apicopilot.window;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import io.apicopilot.codegen.ui.CodeEditorPanel;
import io.apicopilot.document.Document;
import io.apicopilot.model.Property;
import io.apicopilot.model.Request;
import io.apicopilot.util.OpenApiUtils;
import io.apicopilot.window.debug.MethodBadge;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.*;

/**
 * Native-UI API document preview pane.
 */
public class ApiViewPreviewPane extends JPanel implements Disposable {

    @Getter private Document document;
    @Getter private Request  request;

    private final Project  project;
    private final Runnable onDebugClick;

    /** Syntax-highlighted JSON example editor; recreated on each setRequest call. */
    private CodeEditorPanel exampleEditor;

    public ApiViewPreviewPane(Project project, Runnable onDebugClick) {
        super(new BorderLayout());
        this.project      = project;
        this.onDebugClick = onDebugClick;
    }

    public void setRequest(Document document, Request request) {
        // Dispose previous editor before removing components
        if (exampleEditor != null) {
            exampleEditor.dispose();
            exampleEditor = null;
        }

        this.document = document;
        this.request  = request;
        removeAll();

        Operation op = request.getOperation();

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(JBUI.Borders.empty(4, 12, 20, 12));

        body.add(buildHeader(request));
        addRequestSection(body, request, op);

        // Sort responses once, reuse across sections
        List<Map.Entry<String, ApiResponse>> sortedResponses = Collections.emptyList();
        if (OpenApiUtils.hasResponseBody(op)) {
            sortedResponses = new ArrayList<>(op.getResponses().entrySet());
            sortedResponses.sort(Comparator.comparingInt(e -> statusSortKey(e.getKey())));
        }

        addResponsesSection(body, sortedResponses);
        body.add(Box.createVerticalGlue());

        JBScrollPane scroll = new JBScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
        add(scroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    @Override
    public void dispose() {
        if (exampleEditor != null) {
            exampleEditor.dispose();
            exampleEditor = null;
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader(Request request) {
        Operation op = request.getOperation();
        boolean deprecated = Boolean.TRUE.equals(op.getDeprecated());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(10, 0, 10, 0));

        // Row 1: summary + DEPRECATED badge
        {
            JPanel summaryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            summaryRow.setOpaque(false);
            summaryRow.setAlignmentX(LEFT_ALIGNMENT);

            String summary = op.getSummary();
            if (summary != null && !summary.isEmpty()) {
                String text = deprecated ? "<html><s>" + esc(summary) + "</s></html>" : summary;
                JBLabel sl = new JBLabel(text);
                sl.setFont(sl.getFont().deriveFont(Font.BOLD, JBUI.scaleFontSize(14f)));
                summaryRow.add(sl);
            }

            panel.add(summaryRow);
            panel.add(Box.createVerticalStrut(JBUI.scale(5)));
        }

        // Row 2: [Method] path  — hgap=0 to align with row 3
        {
            JPanel methodRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            methodRow.setOpaque(false);
            methodRow.setAlignmentX(LEFT_ALIGNMENT);

            MethodBadge badge = new MethodBadge();
            badge.setMethod(request.getMethod());
            badge.setFont(badge.getFont().deriveFont(Font.BOLD, JBUI.scaleFontSize(11f)));
            badge.setBorder(JBUI.Borders.empty(0, 0, 0, 6)); // right gap before path
            methodRow.add(badge);

            String path = request.getPath();
            DashedUnderlineLabel pathLabel = new DashedUnderlineLabel(path) {
                private boolean hovered = false;
                {
                    setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(12f)));
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hovered = true; setDashedUnderline(true); }
                        @Override public void mouseExited(MouseEvent e)  { hovered = false; setDashedUnderline(false); }
                        @Override public void mouseClicked(MouseEvent e) {
                            CopyPasteManager.getInstance().setContents(new StringSelection(path));
                            showCopiedHint((Component) e.getSource(), e);
                        }
                    });
                }
            };
            methodRow.add(pathLabel);

            panel.add(methodRow);
        }

        // Row 3: description (skip if identical to summary)
        String desc    = op.getDescription();
        String summary = op.getSummary();
        boolean descDifferent = desc != null && !desc.isEmpty()
                && !desc.equals(summary);
        if (descDifferent) {
            panel.add(Box.createVerticalStrut(JBUI.scale(6)));
            JTextArea dl = new JTextArea(desc);
            dl.setEditable(false);
            dl.setOpaque(false);
            dl.setLineWrap(true);
            dl.setWrapStyleWord(true);
            dl.setForeground(UIManager.getColor("Label.disabledForeground"));
            dl.setFont(UIManager.getFont("Label.font"));
            dl.setBorder(null);
            dl.setAlignmentX(LEFT_ALIGNMENT);
            panel.add(dl);
        }

        return panel;
    }

    // ── Request section (Header / Cookie / Path / Query / Body) ──────────────

    private static void addRequestSection(JPanel body, Request request, Operation op) {
        List<Parameter> header = request.getParametersIn(ParameterIn.HEADER);
        List<Parameter> cookie = request.getParametersIn(ParameterIn.COOKIE);
        List<Parameter> path   = request.getParametersIn(ParameterIn.PATH);
        List<Parameter> query  = request.getParametersIn(ParameterIn.QUERY);
        boolean hasBody = OpenApiUtils.hasRequestBody(op);

        if (header.isEmpty() && cookie.isEmpty() && path.isEmpty() && query.isEmpty() && !hasBody) return;

        body.add(sectionDivider("Request"));

        addParamGroup(body, "Header", header);
        addParamGroup(body, "Cookie", cookie);
        addParamGroup(body, "Path",   path);
        addParamGroup(body, "Query",  query);

        if (hasBody) {
            op.getRequestBody().getContent().forEach((contentType, mediaType) -> {
                if (mediaType == null || mediaType.getSchema() == null) return;
                body.add(groupContentTypeLabel("Body", contentType));
                body.add(buildSchemaTree(mediaType.getSchema()));
            });
        }
    }

    private static void addParamGroup(JPanel body, String label, List<Parameter> params) {
        if (params.isEmpty()) return;
        body.add(groupLabel(label));
        for (Parameter p : params) {
            boolean req = Boolean.TRUE.equals(p.getRequired());
            boolean dep = Boolean.TRUE.equals(p.getDeprecated());
            body.add(buildRow(p.getName(), req, dep, paramType(p),
                    OpenApiUtils.getParameterDescriptionMore(p), 0));
            if (p.getSchema() != null) {
                List<?> enumValues = p.getSchema().getEnum();
                if (enumValues != null && !enumValues.isEmpty()) {
                    body.add(buildEnumRow(enumValues, 0));
                }
            }
        }
    }

    // ── Responses (tab per status code) ───────────────────────────────────────

    /**
     * Adds the RESPONSES section and returns the JTabbedPane (multi-response) or null (single/none).
     */
    private static void addResponsesSection(JPanel body,
                                            List<Map.Entry<String, ApiResponse>> sorted) {
        if (sorted.isEmpty()) return;

        body.add(sectionDivider("Response"));

        if (sorted.size() == 1) {
            // No tab bar for single response — show content-type inside the content panel
            body.add(buildResponseContent(sorted.get(0).getValue(), true));
            return;
        }

        // ── Custom tab bar with trailing content-type label ──────────────
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        wrapper.setOpaque(false);

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        JPanel tabButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabButtonsPanel.setOpaque(false);

        final JBLabel ctLabel = new JBLabel("");
        ctLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(10f)));
        Color infoFg = UIManager.getColor("Label.infoForeground");
        if (infoFg == null) infoFg = UIManager.getColor("Label.disabledForeground");
        ctLabel.setForeground(infoFg);
        ctLabel.setBorder(JBUI.Borders.empty(0, 4, 0, 8));

        JPanel tabBar = new JPanel(new BorderLayout(0, 0));
        tabBar.setBorder(JBUI.Borders.customLine(UIManager.getColor("Separator.separatorColor"), 0, 0, 1, 0));
        tabBar.add(tabButtonsPanel, BorderLayout.WEST);
        tabBar.add(ctLabel, BorderLayout.EAST);

        ButtonGroup bg = new ButtonGroup();
        boolean first = true;
        for (Map.Entry<String, ApiResponse> entry : sorted) {
            String      code     = entry.getKey();
            ApiResponse response = entry.getValue();
            if (response == null) continue;

            String ct = primaryContentType(response);
            cardPanel.add(buildResponseContent(response, false), code);

            JToggleButton btn = new JToggleButton(code);
            styleResponseTabButton(btn, false);
            bg.add(btn);

            final String finalCt = ct;
            btn.addActionListener(e -> {
                cardLayout.show(cardPanel, code);
                ctLabel.setText(finalCt != null ? finalCt : "");
                ctLabel.setVisible(finalCt != null);
                for (Component c : tabButtonsPanel.getComponents()) {
                    if (c instanceof JToggleButton)
                        styleResponseTabButton((JToggleButton) c, c == btn);
                }
            });

            if (first) {
                btn.setSelected(true);
                styleResponseTabButton(btn, true);
                ctLabel.setText(ct != null ? ct : "");
                ctLabel.setVisible(ct != null);
                first = false;
            }
            tabButtonsPanel.add(btn);
        }

        wrapper.add(tabBar, BorderLayout.NORTH);
        wrapper.add(cardPanel, BorderLayout.CENTER);
        body.add(wrapper);
    }

    private static JPanel buildResponseContent(ApiResponse response, boolean showContentType) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(4, 0, 4, 0));

        boolean hasContent = response.getContent() != null && !response.getContent().isEmpty();
        if (hasContent) {
            response.getContent().forEach((ct, mt) -> {
                if (mt != null && mt.getSchema() != null) {
                    if (showContentType && !isWildcardContentType(ct)) {
                        panel.add(contentTypeLabel(ct));
                    }
                    panel.add(buildSchemaTree(mt.getSchema()));
                }
            });
        } else {
            String desc = response.getDescription();
            if (desc != null && !desc.isEmpty()) {
                JLabel dl = new JLabel(desc);
                dl.setForeground(UIManager.getColor("Label.disabledForeground"));
                dl.setBorder(JBUI.Borders.empty(2, 14, 2, 0));
                dl.setAlignmentX(LEFT_ALIGNMENT);
                panel.add(dl);
            }
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static void styleResponseTabButton(JToggleButton btn, boolean selected) {
        btn.setFont(btn.getFont().deriveFont(12f));
        btn.setMargin(JBUI.insets(4, 10));
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        if (selected) {
            btn.setForeground(UIManager.getColor("Label.foreground"));
            Color accent = UIManager.getColor("TabbedPane.underlineColor");
            if (accent == null) accent = UIManager.getColor("Button.focusedBorderColor");
            btn.setBorder(JBUI.Borders.customLine(accent, 0, 0, 2, 0));
        } else {
            btn.setForeground(UIManager.getColor("Label.disabledForeground"));
            btn.setBorder(JBUI.Borders.empty(0, 0, 2, 0));
        }
    }

    /** Returns the first non-wildcard content-type of a response, or null. */
    private static String primaryContentType(ApiResponse response) {
        if (response.getContent() == null) return null;
        for (String ct : response.getContent().keySet()) {
            if (!isWildcardContentType(ct)) return ct;
        }
        return null;
    }

    private static boolean isWildcardContentType(String ct) {
        return ct == null || ct.equals("*/*") || ct.endsWith("/*");
    }

    // ── Field tree ────────────────────────────────────────────────────────────

    private static Tree buildSchemaTree(Schema<?> schema) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
        addSchemaNodes(root, schema);

        Tree tree = new Tree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(-1);
        tree.setOpaque(false);
        tree.setBorder(JBUI.Borders.empty(0, 0, 4, 0));
        tree.setAlignmentX(LEFT_ALIGNMENT);

        // No selection
        tree.setSelectionModel(new DefaultTreeSelectionModel() {
            @Override public void setSelectionPath(TreePath p) {}
            @Override public void setSelectionPaths(TreePath[] p) {}
            @Override public void addSelectionPath(TreePath p) {}
            @Override public void addSelectionPaths(TreePath[] p) {}
        });

        // Click on any row copies the field name; hover shows underline
        tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int[] hoveredRow = {-1};
        tree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row != hoveredRow[0]) {
                    hoveredRow[0] = row;
                    tree.repaint();
                }
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredRow[0] != -1) { hoveredRow[0] = -1; tree.repaint(); }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;
                Object node = path.getLastPathComponent();
                if (!(node instanceof DefaultMutableTreeNode)) return;
                Object userObj = ((DefaultMutableTreeNode) node).getUserObject();
                if (!(userObj instanceof SchemaNode)) return;
                String name = ((SchemaNode) userObj).name;
                if (name == null || name.isEmpty() || "(value)".equals(name)) return;
                CopyPasteManager.getInstance().setContents(new StringSelection(name));
                showCopiedHint(tree, e);
            }
        });

        tree.setCellRenderer(new SchemaTreeCellRenderer(hoveredRow));
        expandAllNodes(tree, new TreePath(root));
        return tree;
    }

    private static void addSchemaNodes(DefaultMutableTreeNode parent, Schema<?> schema) {
        if (schema == null) return;
        Property wrapper = new Property(null, schema, false);
        if (wrapper.isObjectType()) {
            for (Property p : wrapper.getObjectProperties()) addPropertyNode(parent, p);
        } else if (wrapper.isArrayObjectType() || (wrapper.isArrayType() && schema.getItems() != null)) {
            addSchemaNodes(parent, schema.getItems());
        } else {
            parent.add(new DefaultMutableTreeNode(new SchemaNode(
                    "(value)", false, false,
                    schemaType(schema), OpenApiUtils.getSchemaDescriptionMore(schema), null)));
        }
    }

    private static void addPropertyNode(DefaultMutableTreeNode parent, Property property) {
        boolean req  = property.isRequired();
        boolean dep  = Boolean.TRUE.equals(property.getSchema().getDeprecated());
        String  name = property.getName() != null ? property.getName() : "";
        String  type = schemaType(property.getSchema());
        String  desc = OpenApiUtils.getSchemaDescriptionMore(property.getSchema());
        List<?> enumValues = property.getSchema().getEnum();

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                new SchemaNode(name, req, dep, type, desc, enumValues));
        parent.add(node);

        boolean hasChildren = property.isObjectType()
                || property.isArrayObjectType()
                || property.isMultipleArrayType();
        if (hasChildren) {
            if (property.isObjectType()) {
                addSchemaNodes(node, property.getSchema());
            } else {
                addSchemaNodes(node, property.getSchema().getItems());
            }
        }
    }

    private static void expandAllNodes(Tree tree, TreePath path) {
        TreeNode node = (TreeNode) path.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            expandAllNodes(tree, path.pathByAddingChild(node.getChildAt(i)));
        }
        tree.expandPath(path);
    }

    // ── Row builders ──────────────────────────────────────────────────────────

    private static JPanel buildRow(String name, boolean required, boolean deprecated,
                                   String type, String desc, int depth) {
        JPanel row = baseRow(depth);
        row.add(Box.createHorizontalStrut(JBUI.scale(16)));
        addRowContent(row, name, required, deprecated, type, desc);
        return row;
    }

private static JPanel baseRow(int depth) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setOpaque(false);
        row.setBorder(JBUI.Borders.empty(2, depth * 16, 2, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(26)));
        return row;
    }

    /** Renders enum values as a row of small chips immediately below a field row. */
    private static JPanel buildEnumRow(List<?> enumValues, int depth) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(2))) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        // Align with description area: field-row indent (14) + depth indent + toggle/strut (14)
        row.setBorder(JBUI.Borders.empty(0, 14 + depth * 16, 4, 0));

        for (Object val : enumValues) {
            if (val == null) continue;
            String text = val.toString();
            JLabel chip = new JLabel(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(JBColor.isBright() ? new Color(0xE8E8E8) : new Color(0x3C3F41));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), JBUI.scale(4), JBUI.scale(4));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chip.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(10f)));
            chip.setForeground(UIManager.getColor("Label.foreground"));
            chip.setBorder(JBUI.Borders.empty(1, 5, 1, 5));
            chip.setOpaque(false);
            row.add(chip);
        }
        return row;
    }

    private static DashedUnderlineLabel addRowContent(JPanel row, String name, boolean required,
                                         boolean deprecated, String type, String desc) {
        // Name — clickable, copies field name to clipboard
        DashedUnderlineLabel nameLabel = makeCopyableNameLabel(name, deprecated);
        row.add(nameLabel);
        row.add(Box.createHorizontalStrut(JBUI.scale(6)));

        // Type chip
        if (type != null && !type.isEmpty()) {
            row.add(new TypeChip(type));
            row.add(Box.createHorizontalStrut(JBUI.scale(5)));
        }

        // Required marker
        if (required) {
            JLabel req = new JLabel("*");
            req.setFont(req.getFont().deriveFont(Font.BOLD, JBUI.scaleFontSize(13f)));
            req.setForeground(new JBColor(new Color(0xCC3333), new Color(0xFF6666)));
            req.setPreferredSize(new Dimension(JBUI.scale(10), JBUI.scale(20)));
            req.setMinimumSize(req.getPreferredSize());
            req.setMaximumSize(req.getPreferredSize());
            row.add(req);
            row.add(Box.createHorizontalStrut(JBUI.scale(6)));
        } else {
            row.add(Box.createHorizontalStrut(JBUI.scale(16)));
        }

        // Description — flexible, tooltip shows full text
        if (desc != null && !desc.isEmpty()) {
            JBLabel dl = new JBLabel(desc);
            dl.setForeground(UIManager.getColor("Label.disabledForeground"));
            dl.setToolTipText(desc);
            row.add(dl);
        }

        row.add(Box.createHorizontalGlue());
        return nameLabel;
    }

    // ── Section divider ───────────────────────────────────────────────────────

    private static TitledSeparator sectionDivider(String text) {
        TitledSeparator sep = new TitledSeparator(text) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        sep.setAlignmentX(LEFT_ALIGNMENT);
        sep.setBorder(JBUI.Borders.empty(12, 0, 4, 0));
        return sep;
    }

    private static JPanel groupLabel(String text) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setOpaque(false);
        p.setBorder(JBUI.Borders.empty(6, 0, 3, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(24)));

        JBLabel l = new JBLabel(text);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, l.getFont().getSize() - 1f));
        Color fg = UIManager.getColor("Label.infoForeground");
        if (fg == null) fg = UIManager.getColor("Label.disabledForeground");
        l.setForeground(fg);
        p.add(l);
        p.add(Box.createHorizontalGlue());
        return p;
    }

    /** Combined "Body  application/json" label on a single row. */
    private static JPanel groupContentTypeLabel(String group, String contentType) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setOpaque(false);
        p.setBorder(JBUI.Borders.empty(6, 0, 3, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(24)));

        Color fg = UIManager.getColor("Label.infoForeground");
        if (fg == null) fg = UIManager.getColor("Label.disabledForeground");

        JBLabel groupLbl = new JBLabel(group);
        groupLbl.setFont(groupLbl.getFont().deriveFont(Font.ITALIC, groupLbl.getFont().getSize() - 1f));
        groupLbl.setForeground(fg);

        JBLabel ctLbl = new JBLabel("  " + contentType);
        ctLbl.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(11f)));
        ctLbl.setForeground(fg);

        p.add(groupLbl);
        p.add(ctLbl);
        p.add(Box.createHorizontalGlue());
        return p;
    }

    /** Content-type sub-label under Body group (e.g. "application/json"). */
    private static JPanel contentTypeLabel(String contentType) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setOpaque(false);
        p.setBorder(JBUI.Borders.empty(3, 0, 2, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(20)));

        JBLabel l = new JBLabel(contentType);
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(11f)));
        Color fg = UIManager.getColor("Label.infoForeground");
        if (fg == null) fg = UIManager.getColor("Label.disabledForeground");
        l.setForeground(fg);
        p.add(l);
        p.add(Box.createHorizontalGlue());
        return p;
    }

    // ── Type helpers ──────────────────────────────────────────────────────────

    private static String schemaType(Schema<?> schema) {
        if (schema == null) return "";
        String type = schema.getType();
        if ("array".equals(type) && schema.getItems() != null) {
            String itemType = schema.getItems().getType();
            return (itemType != null ? itemType : "object") + "[]";
        }
        if (type == null) return "object";
        String fmt = schema.getFormat();
        return (fmt != null && !fmt.isEmpty()) ? type + "<" + fmt + ">" : type;
    }

    private static String paramType(Parameter p) {
        return p.getSchema() != null ? schemaType(p.getSchema()) : "";
    }

    private static int parseStatus(String code) {
        try { return Integer.parseInt(code); } catch (NumberFormatException e) { return 0; }
    }

    private static int statusSortKey(String code) {
        int s = parseStatus(code);
        return s > 0 ? s : Integer.MAX_VALUE;
    }

    /** Creates a bold field-name label that copies the name on click and underlines on hover. */
    private static DashedUnderlineLabel makeCopyableNameLabel(String rawName, boolean deprecated) {
        String display = deprecated ? "<html><s>" + esc(rawName) + "</s></html>" : rawName;
        DashedUnderlineLabel label = new DashedUnderlineLabel(display);
        Font boldFont = label.getFont().deriveFont(Font.BOLD);
        label.setFont(boldFont);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!deprecated) {
                    label.setDashedUnderline(true);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                label.setDashedUnderline(false);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(rawName));
                showCopiedHint(label, e);
            }
        });

        return label;
    }

    private static class DashedUnderlineLabel extends JBLabel {
        private boolean dashedUnderline;

        DashedUnderlineLabel(String text) {
            super(text);
        }

        void setDashedUnderline(boolean dashedUnderline) {
            this.dashedUnderline = dashedUnderline;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!dashedUnderline) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(getForeground());
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        1f, new float[]{2f, 2f}, 0f));
                FontMetrics fm = g2.getFontMetrics(getFont());
                int y = fm.getAscent() + fm.getDescent();
                int width = Math.min(fm.stringWidth(getText()), getWidth());
                g2.drawLine(0, y, width, y);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Shows a lightweight "Copied!" tooltip near the cursor, auto-dismissed after 1.2s. */
    private static void showCopiedHint(Component near, MouseEvent e) {
        Window owner = SwingUtilities.getWindowAncestor(near);
        JWindow hint = new JWindow(owner);
        boolean bright = JBColor.isBright();
        Color bg = bright ? new Color(0xFFFDF7) : new Color(0x2F332B);
        Color border = bright ? new Color(0xD8D2BE) : new Color(0x4B5345);
        Color fg = bright ? new Color(0x2E7D4F) : new Color(0x7ED6A3);

        JPanel bubble = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), JBUI.scale(10), JBUI.scale(10));
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, JBUI.scale(10), JBUI.scale(10));
                } finally {
                    g2.dispose();
                }
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(JBUI.Borders.empty(4, 10));

        JLabel msg = new JLabel("Copied!");
        msg.setFont(msg.getFont().deriveFont(Font.BOLD, 11f));
        msg.setForeground(fg);
        msg.setOpaque(false);
        bubble.add(msg, BorderLayout.CENTER);

        hint.setBackground(new Color(0, 0, 0, 0));
        hint.setContentPane(bubble);
        hint.pack();
        hint.setShape(new RoundRectangle2D.Double(
                0, 0, hint.getWidth(), hint.getHeight(), JBUI.scale(10), JBUI.scale(10)));

        Point p = e.getLocationOnScreen();
        hint.setLocation(p.x + 6, p.y - hint.getHeight() - 6);
        hint.setVisible(true);

        javax.swing.Timer t = new javax.swing.Timer(1200, ev -> hint.dispose());
        t.setRepeats(false);
        t.start();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ── Inner: SchemaNode ─────────────────────────────────────────────────────

    private static final class SchemaNode {
        final String  name;
        final boolean required;
        final boolean deprecated;
        final String  type;
        final String  desc;
        final List<?> enumValues;

        SchemaNode(String name, boolean required, boolean deprecated,
                   String type, String desc, List<?> enumValues) {
            this.name       = name;
            this.required   = required;
            this.deprecated = deprecated;
            this.type       = type;
            this.desc       = desc;
            this.enumValues = enumValues;
        }
    }

    // ── Inner: SchemaTreeCellRenderer ─────────────────────────────────────────

    private static class SchemaTreeCellRenderer implements TreeCellRenderer {
        private final int[] hoveredRow;

        SchemaTreeCellRenderer(int[] hoveredRow) {
            this.hoveredRow = hoveredRow;
        }

        @Override
        public Component getTreeCellRendererComponent(javax.swing.JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (!(value instanceof DefaultMutableTreeNode)) return new JLabel();
            Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
            if (!(userObj instanceof SchemaNode)) return new JLabel();

            SchemaNode data = (SchemaNode) userObj;

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setOpaque(false);

            // Field row
            JPanel topRow = new JPanel();
            topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
            topRow.setOpaque(false);
            topRow.setBorder(JBUI.Borders.empty(2, 0, 2, 8));
            DashedUnderlineLabel nameLabel = (DashedUnderlineLabel) addRowContent(topRow, data.name, data.required, data.deprecated, data.type, data.desc);
            // Apply underline when this row is hovered
            if (hoveredRow[0] == row && !data.deprecated) {
                nameLabel.setDashedUnderline(true);
            } else {
                nameLabel.setDashedUnderline(false);
            }
            panel.add(topRow);

            // Enum chips
            if (data.enumValues != null && !data.enumValues.isEmpty()) {
                JPanel enumRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(2))) {
                    @Override public Dimension getMaximumSize() {
                        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                    }
                };
                enumRow.setOpaque(false);
                enumRow.setAlignmentX(LEFT_ALIGNMENT);
                enumRow.setBorder(JBUI.Borders.empty(0, 0, 4, 0));
                for (Object val : data.enumValues) {
                    if (val == null) continue;
                    JLabel chip = new JLabel(val.toString()) {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(JBColor.isBright() ? new Color(0xE8E8E8) : new Color(0x3C3F41));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), JBUI.scale(4), JBUI.scale(4));
                            g2.dispose();
                            super.paintComponent(g);
                        }
                    };
                    chip.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(10f)));
                    chip.setForeground(UIManager.getColor("Label.foreground"));
                    chip.setBorder(JBUI.Borders.empty(1, 5, 1, 5));
                    chip.setOpaque(false);
                    enumRow.add(chip);
                }
                panel.add(enumRow);
            }

            return panel;
        }
    }

    // ── Inner: TypeChip ───────────────────────────────────────────────────────

    private static class TypeChip extends JLabel {

        private final Color bg;

        TypeChip(String type) {
            super(displayType(type));
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(10f)));
            setBorder(JBUI.Borders.empty(1, 5, 1, 5));
            setOpaque(false);

            boolean dark = !JBColor.isBright();
            Color[] colors = typeColors(baseType(type), dark);
            this.bg = colors[0];
            setForeground(colors[1]);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }

        private static String baseType(String type) {
            if (type == null) return "any";
            int lt = type.indexOf('<');
            String base = lt > 0 ? type.substring(0, lt) : type;
            return base.replace("[]", "");
        }

        private static String displayType(String type) {
            if (type == null) return "any";
            int lt = type.indexOf('<');
            if (lt > 0) {
                String base = type.substring(0, lt);
                String fmt  = type.substring(lt + 1, type.length() - 1);
                return base + " <" + fmt + ">";
            }
            return type;
        }

        private static Color[] typeColors(String base, boolean dark) {
            switch (base) {
                case "string":
                    return dark ? new Color[]{new Color(0x1B3A5C), new Color(0x6AADDC)}
                                : new Color[]{new Color(0xDBEEFC), new Color(0x1A5F9A)};
                case "integer": case "number":
                    return dark ? new Color[]{new Color(0x5C3A1B), new Color(0xDCA06A)}
                                : new Color[]{new Color(0xFCEEDB), new Color(0x9A5F1A)};
                case "boolean":
                    return dark ? new Color[]{new Color(0x3A1B5C), new Color(0xA06ADC)}
                                : new Color[]{new Color(0xEEDBFC), new Color(0x5F1A9A)};
                default:
                    return dark ? new Color[]{new Color(0x383838), new Color(0xAAAAAA)}
                                : new Color[]{new Color(0xEEEEEE), new Color(0x555555)};
            }
        }
    }
}
