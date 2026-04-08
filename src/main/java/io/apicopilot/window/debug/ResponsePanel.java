package io.apicopilot.window.debug;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import io.apicopilot.codegen.ui.CodeEditorPanel;
import io.apicopilot.debug.DebugHttpClient;
import io.apicopilot.debug.DebugHttpResponse;

import javax.swing.*;
import java.awt.*;

/**
 * Apifox-style response panel.
 *
 * <pre>
 * ┌────────────────────────────────────────────────────────────┐
 * │ Response                                                   │  title bar
 * ├────────────────────────────────────────────────────────────┤
 * │ Body(tab) │ Headers(tab)    [200 OK] · [234ms] · [1.2KB]  │  tab bar
 * │ ┌──────────────────────────────────────────────────────┐   │
 * │ │ [Pretty] [Raw]                         body toolbar  │   │
 * │ │ ┌────────────────────────────────────────────────┐   │   │
 * │ │ │  idle / loading / pretty / raw / error         │   │   │
 * │ │ └────────────────────────────────────────────────┘   │   │
 * │ └──────────────────────────────────────────────────────┘   │
 * └────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class ResponsePanel extends JPanel implements Disposable {

    // Body display cards
    private static final String CARD_IDLE    = "idle";
    private static final String CARD_LOADING = "loading";
    private static final String CARD_PRETTY  = "pretty";
    private static final String CARD_RAW     = "raw";
    private static final String CARD_ERROR   = "error";

    // Tab bar status widgets
    private final JLabel statusBadge = new JBLabel();
    private final JLabel timeLabel   = new JBLabel();
    private final JLabel sizeLabel   = new JBLabel();
    private final JLabel dotTime     = new JBLabel("·");
    private final JLabel dotSize     = new JBLabel("·");

    // Custom tab buttons
    private final JToggleButton bodyTabBtn    = new JToggleButton("Body");
    private final JToggleButton headersTabBtn = new JToggleButton("Headers");
    private final CardLayout tabContentLayout = new CardLayout();
    private final JPanel tabContentPanel      = new JPanel(tabContentLayout);

    private static final String TAB_BODY    = "body";
    private static final String TAB_HEADERS = "headers";

    // Body toolbar
    private final JPanel bodyToolbar;
    private final JToggleButton prettyBtn = new JToggleButton("Pretty");
    private final JToggleButton rawBtn    = new JToggleButton("Raw");

    // Body cards
    private final CardLayout bodyCardLayout = new CardLayout();
    private final JPanel bodyCardPanel = new JPanel(bodyCardLayout);
    private final CodeEditorPanel prettyEditor;
    private final JBTextArea rawTextArea = new JBTextArea();
    private final JLabel errorLabel = new JBLabel();

    // Other tabs
    private final ResponseHeadersPanel headersPanel = new ResponseHeadersPanel();

    // Cached response data
    private String cachedRawBody = "";
    private String cachedFormattedBody = "";
    private boolean cachedIsJson = false;

    // Collapse toggle
    private JButton collapseBtn;
    private Runnable collapseToggleCallback;

    public ResponsePanel(Project project) {
        super(new BorderLayout());

        prettyEditor = new CodeEditorPanel(project, true);

        // ── Title bar ────────────────────────────────────────────────────
        add(buildTitleBar(), BorderLayout.NORTH);

        // ── Body toolbar (Pretty / Raw) ───────────────────────────────────
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(prettyBtn);
        btnGroup.add(rawBtn);
        prettyBtn.setSelected(true);
        styleToggleButton(prettyBtn);
        styleToggleButton(rawBtn);
        prettyBtn.addActionListener(e -> applyPretty());
        rawBtn.addActionListener(e -> applyRaw());

        bodyToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 3));
        bodyToolbar.setBorder(JBUI.Borders.customLine(UIManager.getColor("Separator.separatorColor"), 0, 0, 1, 0));
        bodyToolbar.add(prettyBtn);
        bodyToolbar.add(rawBtn);
        bodyToolbar.setVisible(false);

        // ── Body cards ────────────────────────────────────────────────────
        JBLabel idleLabel = new JBLabel("Send a request to see the response");
        idleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        idleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        JBLabel loadingLabel = new JBLabel("Sending…");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        errorLabel.setForeground(JBColor.RED);

        rawTextArea.setEditable(false);
        rawTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(12f)));
        rawTextArea.setLineWrap(true);
        rawTextArea.setWrapStyleWord(true);
        JBScrollPane rawScroll = new JBScrollPane(rawTextArea);
        rawScroll.setBorder(JBUI.Borders.empty());

        bodyCardPanel.add(idleLabel, CARD_IDLE);
        bodyCardPanel.add(loadingLabel, CARD_LOADING);
        bodyCardPanel.add(prettyEditor, CARD_PRETTY);
        bodyCardPanel.add(rawScroll, CARD_RAW);
        bodyCardPanel.add(errorLabel, CARD_ERROR);

        // ── Body tab ──────────────────────────────────────────────────────
        JPanel bodyTab = new JPanel(new BorderLayout());
        bodyTab.add(bodyToolbar, BorderLayout.NORTH);
        bodyTab.add(bodyCardPanel, BorderLayout.CENTER);

        // ── Tab content cards ─────────────────────────────────────────────
        tabContentPanel.add(bodyTab, TAB_BODY);
        tabContentPanel.add(headersPanel, TAB_HEADERS);

        // ── Custom tab bar row ────────────────────────────────────────────
        ButtonGroup tabGroup = new ButtonGroup();
        tabGroup.add(bodyTabBtn);
        tabGroup.add(headersTabBtn);
        bodyTabBtn.setSelected(true);
        styleTabButton(bodyTabBtn, true);
        styleTabButton(headersTabBtn, false);
        bodyTabBtn.addActionListener(e -> {
            tabContentLayout.show(tabContentPanel, TAB_BODY);
            styleTabButton(bodyTabBtn, true);
            styleTabButton(headersTabBtn, false);
        });
        headersTabBtn.addActionListener(e -> {
            tabContentLayout.show(tabContentPanel, TAB_HEADERS);
            styleTabButton(headersTabBtn, true);
            styleTabButton(bodyTabBtn, false);
        });

        JPanel tabBar = buildTabBar();

        JPanel centerArea = new JPanel(new BorderLayout(0, 0));
        centerArea.add(tabBar, BorderLayout.NORTH);
        centerArea.add(tabContentPanel, BorderLayout.CENTER);

        add(centerArea, BorderLayout.CENTER);

        showIdle();
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setCollapseToggleCallback(Runnable callback) {
        this.collapseToggleCallback = callback;
    }

    /** Called by the parent to sync button icon after external splitter resize. */
    public void updateCollapseButtonState(boolean collapsed) {
        if (collapseBtn == null) return;
        if (collapsed) {
            collapseBtn.setIcon(AllIcons.General.ArrowUp);
            collapseBtn.setToolTipText("Expand");
        } else {
            collapseBtn.setIcon(AllIcons.General.ArrowDown);
            collapseBtn.setToolTipText("Collapse");
        }
    }

    public void showIdle() {
        setStatusVisible(false);
        bodyToolbar.setVisible(false);
        bodyCardLayout.show(bodyCardPanel, CARD_IDLE);
        headersPanel.clear();
    }

    public void showLoading() {
        setStatusVisible(false);
        bodyToolbar.setVisible(false);
        bodyCardLayout.show(bodyCardPanel, CARD_LOADING);
    }

    public void showResponse(DebugHttpResponse response) {
        if (!response.isSuccess()) {
            showError(response.getErrorMessage());
            return;
        }

        int code = response.getStatusCode();
        statusBadge.setText(response.getStatusText());
        statusBadge.setForeground(statusColor(code));
        timeLabel.setText(response.getTimeText());
        sizeLabel.setText(response.getSizeText());
        setStatusVisible(true);

        // Cache body data
        cachedRawBody       = response.getBody() != null ? response.getBody() : "";
        cachedIsJson        = response.isJsonResponse();
        cachedFormattedBody = cachedIsJson
                ? DebugHttpClient.formatJson(cachedRawBody)
                : cachedRawBody;

        headersPanel.setHeaders(response.getHeaders());

        // Apply current pretty/raw toggle
        bodyToolbar.setVisible(true);
        if (prettyBtn.isSelected()) applyPretty();
        else applyRaw();
    }

    public void showError(String message) {
        setStatusVisible(false);
        bodyToolbar.setVisible(false);
        errorLabel.setText("<html><center>Request Failed<br/><small>" +
                (message != null ? message : "Unknown error") + "</small></center></html>");
        bodyCardLayout.show(bodyCardPanel, CARD_ERROR);
        headersPanel.clear();
    }

    @Override
    public void dispose() {
        prettyEditor.dispose();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void applyPretty() {
        prettyEditor.setText(cachedFormattedBody, cachedIsJson ? "json" : "txt");
        bodyCardLayout.show(bodyCardPanel, CARD_PRETTY);
    }

    private void applyRaw() {
        rawTextArea.setText(cachedRawBody);
        rawTextArea.setCaretPosition(0);
        bodyCardLayout.show(bodyCardPanel, CARD_RAW);
    }

    private void setStatusVisible(boolean visible) {
        statusBadge.setVisible(visible);
        timeLabel.setVisible(visible);
        sizeLabel.setVisible(visible);
        dotTime.setVisible(visible);
        dotSize.setVisible(visible);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBorder(JBUI.Borders.merge(
                JBUI.Borders.customLine(UIManager.getColor("Separator.separatorColor"), 1, 0, 0, 0),
                JBUI.Borders.empty(4, 8),
                true));

        JBLabel label = new JBLabel("Response");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        bar.add(label, BorderLayout.WEST);

        // Collapse / expand toggle button
        collapseBtn = new JButton(AllIcons.General.ArrowDown);
        collapseBtn.setToolTipText("Collapse");
        collapseBtn.setBorderPainted(false);
        collapseBtn.setContentAreaFilled(false);
        collapseBtn.setFocusable(false);
        collapseBtn.setMargin(JBUI.insets(2));
        collapseBtn.addActionListener(e -> {
            if (collapseToggleCallback != null) collapseToggleCallback.run();
        });

        // Clicking the title label also triggers collapse
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (collapseToggleCallback != null) collapseToggleCallback.run();
            }
        });

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(collapseBtn);
        bar.add(rightPanel, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildTabBar() {
        // Init status widgets
        statusBadge.setFont(statusBadge.getFont().deriveFont(Font.BOLD, 11f));
        timeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        timeLabel.setFont(timeLabel.getFont().deriveFont(11f));
        sizeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(11f));
        dotTime.setForeground(UIManager.getColor("Label.disabledForeground"));
        dotTime.setFont(dotTime.getFont().deriveFont(11f));
        dotSize.setForeground(UIManager.getColor("Label.disabledForeground"));
        dotSize.setFont(dotSize.getFont().deriveFont(11f));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        statusPanel.setOpaque(false);
        statusPanel.add(statusBadge);
        statusPanel.add(dotTime);
        statusPanel.add(timeLabel);
        statusPanel.add(dotSize);
        statusPanel.add(sizeLabel);

        JPanel tabButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabButtons.setOpaque(false);
        tabButtons.add(bodyTabBtn);
        tabButtons.add(headersTabBtn);

        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBorder(JBUI.Borders.customLine(UIManager.getColor("Separator.separatorColor"), 0, 0, 1, 0));
        bar.add(tabButtons, BorderLayout.WEST);
        bar.add(statusPanel, BorderLayout.EAST);
        return bar;
    }

    private static void styleToggleButton(JToggleButton btn) {
        btn.setFont(btn.getFont().deriveFont(11f));
        btn.setMargin(JBUI.insets(2, 8));
        btn.setFocusable(false);
    }

    private static void styleTabButton(JToggleButton btn, boolean selected) {
        btn.setFont(btn.getFont().deriveFont(12f));
        btn.setMargin(JBUI.insets(4, 10));
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        if (selected) {
            btn.setForeground(UIManager.getColor("Label.foreground"));
            btn.setBorder(JBUI.Borders.customLine(
                    UIManager.getColor("TabbedPane.underlineColor") != null
                            ? UIManager.getColor("TabbedPane.underlineColor")
                            : UIManager.getColor("Button.focusedBorderColor"),
                    0, 0, 2, 0));
        } else {
            btn.setForeground(UIManager.getColor("Label.disabledForeground"));
            btn.setBorder(JBUI.Borders.empty(0, 0, 2, 0));
        }
    }

    private static Color statusColor(int code) {
        if (code >= 200 && code < 300) return new JBColor(new Color(0x008800), new Color(0x00BB00));
        if (code >= 300 && code < 400) return new JBColor(new Color(0x0055CC), new Color(0x4499FF));
        if (code >= 400 && code < 500) return new JBColor(new Color(0xCC6600), new Color(0xFF9900));
        return new JBColor(new Color(0xCC0000), new Color(0xFF4444));
    }
}
