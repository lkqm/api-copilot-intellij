package io.apicopilot.window.debug;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import io.apicopilot.debug.AuthConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Auth tab panel for the API debug tool.
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────┐
 * │  [Inherit from Document ▾]  [⚙]                         │  ← mode row (optional)
 * ├──────────────────────────────────────────────────────────┤
 * │  Auth Type  [Bearer Token ▼]                             │
 * ├──────────────────────────────────────────────────────────┤
 * │  Token      [•••••••••••••••••••••••••••••••]  [Show]   │
 * └──────────────────────────────────────────────────────────┘
 * </pre>
 *
 * When {@code showModeToggle=true} (default, used in debug panel):
 * - Inherit mode: fields filled with document-level auth, all disabled (read-only preview).
 *   A gear icon button is shown to open the document auth editor.
 * - Custom mode:  fields editable, override applies only to current request session.
 *
 * When {@code showModeToggle=false} (used in DocAuthDialog):
 * - Always editable, no mode row shown.
 */
public class AuthPanel extends JPanel {

    private static final String CARD_NONE    = "NONE";
    private static final String CARD_BEARER  = "BEARER";
    private static final String CARD_BASIC   = "BASIC";
    private static final String CARD_API_KEY = "API_KEY";
    private static final String CARD_JWT     = "JWT";
    private static final String CARD_DIGEST  = "DIGEST";

    private static final int LABEL_WIDTH = 80;

    // ── Mode state (showModeToggle=true only) ──────────────────────────────
    private final boolean showModeToggle;
    private boolean inheriting = true;
    private AuthConfig documentAuth = new AuthConfig();
    private Runnable manageDocAuthAction;

    // Mode row widgets — created only when showModeToggle=true
    private ModeDropdownButton modeDropdownBtn;
    private ActionButton       docAuthIconBtn;

    // ── Type selector ──────────────────────────────────────────────────────
    private final JComboBox<AuthConfig.Type> typeCombo = new JComboBox<>(AuthConfig.Type.values());

    // ── Cards ──────────────────────────────────────────────────────────────
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);

    // Bearer Token
    private final JBPasswordField bearerTokenField = new JBPasswordField();

    // Basic Auth
    private final JBTextField     basicUsernameField = new JBTextField();
    private final JBPasswordField basicPasswordField = new JBPasswordField();

    // API Key
    private final JBTextField     apiKeyNameField  = new JBTextField();
    private final JBPasswordField apiKeyValueField = new JBPasswordField();
    private final JRadioButton    apiKeyInHeaderRb = new JRadioButton("Header", true);
    private final JRadioButton    apiKeyInQueryRb  = new JRadioButton("Query Param");

    // JWT
    private final JComboBox<String> jwtAlgorithmCombo = new JComboBox<>(new String[]{"HS256", "HS384", "HS512"});
    private final JBPasswordField   jwtSecretField    = new JBPasswordField();
    private final JBTextArea        jwtPayloadArea    = new JBTextArea(5, 0);
    private final JBTextField       jwtPrefixField    = new JBTextField("Bearer");

    // Digest Auth
    private final JBTextField     digestUsernameField = new JBTextField();
    private final JBPasswordField digestPasswordField = new JBPasswordField();

    // ── Listeners ──────────────────────────────────────────────────────────
    private final List<Runnable> changeListeners = new ArrayList<>();

    /** Default constructor: shows mode toggle (for request debug panel). */
    public AuthPanel() {
        this(true);
    }

    /**
     * @param showModeToggle true  = show Inherit/Custom dropdown (request debug panel);
     *                       false = always editable, no mode row (DocAuthDialog)
     */
    public AuthPanel(boolean showModeToggle) {
        super(new BorderLayout());
        this.showModeToggle = showModeToggle;
        setBorder(JBUI.Borders.empty(10, 8, 8, 8));

        add(buildTopRow(), BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        cardPanel.add(buildNoneCard(),   CARD_NONE);
        cardPanel.add(buildBearerCard(), CARD_BEARER);
        cardPanel.add(buildBasicCard(),  CARD_BASIC);
        cardPanel.add(buildApiKeyCard(), CARD_API_KEY);
        cardPanel.add(buildJwtCard(),    CARD_JWT);
        cardPanel.add(buildDigestCard(), CARD_DIGEST);

        typeCombo.addActionListener(e -> {
            AuthConfig.Type t = (AuthConfig.Type) typeCombo.getSelectedItem();
            if (t != null) {
                switch (t) {
                    case NONE:    cardLayout.show(cardPanel, CARD_NONE);    break;
                    case BEARER:  cardLayout.show(cardPanel, CARD_BEARER);  break;
                    case BASIC:   cardLayout.show(cardPanel, CARD_BASIC);   break;
                    case API_KEY: cardLayout.show(cardPanel, CARD_API_KEY); break;
                    case JWT:     cardLayout.show(cardPanel, CARD_JWT);     break;
                    case DIGEST:  cardLayout.show(cardPanel, CARD_DIGEST);  break;
                }
            }
            notifyChanged();
        });

        addDocListener(bearerTokenField,    this::notifyChanged);
        addDocListener(basicUsernameField,  this::notifyChanged);
        addDocListener(basicPasswordField,  this::notifyChanged);
        addDocListener(apiKeyNameField,     this::notifyChanged);
        addDocListener(apiKeyValueField,    this::notifyChanged);
        addDocListener(jwtSecretField,      this::notifyChanged);
        addDocListener(jwtPrefixField,      this::notifyChanged);
        addDocListener(digestUsernameField, this::notifyChanged);
        addDocListener(digestPasswordField, this::notifyChanged);
        jwtPayloadArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { notifyChanged(); }
            public void removeUpdate(DocumentEvent e)  { notifyChanged(); }
            public void changedUpdate(DocumentEvent e) { notifyChanged(); }
        });
        jwtAlgorithmCombo.addActionListener(e -> notifyChanged());
        apiKeyInHeaderRb.addActionListener(e -> notifyChanged());
        apiKeyInQueryRb.addActionListener(e  -> notifyChanged());

        apiKeyNameField.getEmptyText().setText("e.g. X-API-Key");
        jwtPrefixField.getEmptyText().setText("Bearer");

        // Initial state: inherit mode → fields disabled
        if (showModeToggle) {
            setFieldsEnabled(false);
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Sets the document-level auth and resets to inherit mode.
     * Call this every time a new request is loaded.
     */
    public void setDocumentAuth(AuthConfig docAuth, Runnable manageAction) {
        this.documentAuth        = docAuth != null ? docAuth : new AuthConfig();
        this.manageDocAuthAction = manageAction;
        applyInheritMode();
    }

    /**
     * Refreshes the displayed document auth (e.g. after saving from DocAuthDialog).
     * Only reloads the fields if still in inherit mode.
     */
    public void refreshDocumentAuth(AuthConfig docAuth) {
        this.documentAuth = docAuth != null ? docAuth : new AuthConfig();
        if (inheriting) {
            loadDocAuthIntoFields();
            notifyChanged();
        }
    }

    /** Direct field set — used in dialog mode (showModeToggle=false). */
    public void setAuthConfig(AuthConfig config) {
        if (config == null) return;
        setAuthConfigFields(config);
    }

    public AuthConfig getAuthConfig() {
        AuthConfig config = new AuthConfig();
        config.setType((AuthConfig.Type) typeCombo.getSelectedItem());
        config.setBearerToken(new String(bearerTokenField.getPassword()));
        config.setBasicUsername(basicUsernameField.getText());
        config.setBasicPassword(new String(basicPasswordField.getPassword()));
        config.setApiKeyName(apiKeyNameField.getText());
        config.setApiKeyValue(new String(apiKeyValueField.getPassword()));
        config.setApiKeyInHeader(apiKeyInHeaderRb.isSelected());
        config.setJwtAlgorithm((String) jwtAlgorithmCombo.getSelectedItem());
        config.setJwtSecret(new String(jwtSecretField.getPassword()));
        config.setJwtPayload(jwtPayloadArea.getText());
        config.setJwtPrefix(jwtPrefixField.getText());
        config.setDigestUsername(digestUsernameField.getText());
        config.setDigestPassword(new String(digestPasswordField.getPassword()));
        return config;
    }

    public boolean hasValues() {
        AuthConfig.Type t = (AuthConfig.Type) typeCombo.getSelectedItem();
        return t != null && t != AuthConfig.Type.NONE;
    }

    public boolean isInheriting() {
        return showModeToggle && inheriting;
    }

    public void addValueChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    // ── Mode switching ─────────────────────────────────────────────────────

    private void applyInheritMode() {
        inheriting = true;
        loadDocAuthIntoFields();
        setFieldsEnabled(false);
        syncModeWidgets();
        notifyChanged();
    }

    private void applyCustomMode() {
        inheriting = false;
        setFieldsEnabled(true);
        syncModeWidgets();
        notifyChanged();
    }

    /** Keeps the mode button text and gear icon visibility in sync with {@link #inheriting}. */
    private void syncModeWidgets() {
        if (modeDropdownBtn != null) modeDropdownBtn.repaint();
        if (docAuthIconBtn  != null) docAuthIconBtn.setVisible(inheriting);
    }

    private void showModePopup() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem inheritItem = new JMenuItem((inheriting ? "\u2713 " : "    ") + "Inherit from Document");
        inheritItem.addActionListener(e -> applyInheritMode());
        menu.add(inheritItem);

        JMenuItem customItem = new JMenuItem((!inheriting ? "\u2713 " : "    ") + "Custom");
        customItem.addActionListener(e -> applyCustomMode());
        menu.add(customItem);

        menu.show(modeDropdownBtn, 0, modeDropdownBtn.getHeight());
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void loadDocAuthIntoFields() {
        setAuthConfigFields(documentAuth);
    }

    private void setAuthConfigFields(AuthConfig config) {
        if (config == null) return;
        typeCombo.setSelectedItem(config.getType());
        bearerTokenField.setText(config.getBearerToken());
        basicUsernameField.setText(config.getBasicUsername());
        basicPasswordField.setText(config.getBasicPassword());
        apiKeyNameField.setText(config.getApiKeyName());
        apiKeyValueField.setText(config.getApiKeyValue());
        apiKeyInHeaderRb.setSelected(config.isApiKeyInHeader());
        apiKeyInQueryRb.setSelected(!config.isApiKeyInHeader());
        jwtAlgorithmCombo.setSelectedItem(config.getJwtAlgorithm());
        jwtSecretField.setText(config.getJwtSecret());
        jwtPayloadArea.setText(config.getJwtPayload());
        jwtPrefixField.setText(config.getJwtPrefix());
        digestUsernameField.setText(config.getDigestUsername());
        digestPasswordField.setText(config.getDigestPassword());
    }

    private void setFieldsEnabled(boolean enabled) {
        typeCombo.setEnabled(enabled);
        bearerTokenField.setEnabled(enabled);
        basicUsernameField.setEnabled(enabled);
        basicPasswordField.setEnabled(enabled);
        apiKeyNameField.setEnabled(enabled);
        apiKeyValueField.setEnabled(enabled);
        apiKeyInHeaderRb.setEnabled(enabled);
        apiKeyInQueryRb.setEnabled(enabled);
        jwtAlgorithmCombo.setEnabled(enabled);
        jwtSecretField.setEnabled(enabled);
        jwtPayloadArea.setEnabled(enabled);
        jwtPrefixField.setEnabled(enabled);
        digestUsernameField.setEnabled(enabled);
        digestPasswordField.setEnabled(enabled);
    }

    // ── Top row ────────────────────────────────────────────────────────────

    private JPanel buildTopRow() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(JBUI.Borders.emptyBottom(10));

        if (showModeToggle) {
            container.add(buildModeRow());
            container.add(Box.createVerticalStrut(JBUI.scale(8)));
        }
        container.add(buildTypeRow());

        return container;
    }

    private JPanel buildModeRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setAlignmentX(LEFT_ALIGNMENT);

        // ── Left: mode dropdown button ──
        modeDropdownBtn = new ModeDropdownButton();
        modeDropdownBtn.addActionListener(e -> showModePopup());

        // ── Right: gear icon (inherit mode only) ──
        AnAction settingsAction = new AnAction("Edit document auth", null, AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (manageDocAuthAction != null) manageDocAuthAction.run();
            }
        };
        docAuthIconBtn = new ActionButton(
                settingsAction,
                settingsAction.getTemplatePresentation().clone(),
                ActionPlaces.UNKNOWN,
                JBUI.size(22, 22));

        row.add(modeDropdownBtn, BorderLayout.WEST);
        row.add(docAuthIconBtn,  BorderLayout.EAST);
        return row;
    }

    private JPanel buildTypeRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setAlignmentX(LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.insets  = new Insets(0, 0, 0, JBUI.scale(8));
        gbc.weightx = 0;
        gbc.gridx   = 0;

        JBLabel lbl = new JBLabel("Auth Type");
        lbl.setPreferredSize(new Dimension(JBUI.scale(LABEL_WIDTH), lbl.getPreferredSize().height));
        row.add(lbl, gbc);

        gbc.gridx   = 1;
        gbc.weightx = 1.0;
        gbc.insets  = new Insets(0, 0, 0, 0);
        row.add(typeCombo, gbc);

        return row;
    }

    // ── Mode dropdown button ───────────────────────────────────────────────

    /**
     * Flat text button that reads the current mode and renders
     * "Inherit from Document ▾" or "Custom ▾".
     */
    private class ModeDropdownButton extends JButton {

        private boolean hovered = false;

        ModeDropdownButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            String longestText = "Inherit from Document \u25BE";
            int w = fm.stringWidth(longestText) + JBUI.scale(10);
            int h = fm.getHeight() + JBUI.scale(6);
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth(), h = getHeight();

                // Hover background
                if (hovered || getModel().isPressed()) {
                    Color hover = UIManager.getColor("ActionButton.hoverBackground");
                    if (hover == null) {
                        hover = UIManager.getColor("Button.select");
                    }
                    if (hover != null) {
                        g2.setColor(hover);
                        g2.fillRoundRect(0, 0, w, h, JBUI.scale(4), JBUI.scale(4));
                    }
                }

                // Text
                String label = inheriting ? "Inherit from Document" : "Custom";
                String fullText = label + " \u25BE"; // ▾
                Color fg = UIManager.getColor("Label.foreground");
                if (fg == null) fg = getForeground();
                g2.setFont(getFont());
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(fullText, JBUI.scale(4), textY);

            } finally {
                g2.dispose();
            }
        }
    }

    // ── Card builders ──────────────────────────────────────────────────────

    private JPanel buildNoneCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.emptyTop(24));
        JBLabel hint = new JBLabel("No authentication will be used", SwingConstants.CENTER);
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(hint, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildBearerCard() {
        JPanel form = new JPanel(new GridBagLayout());
        addFormRow(form, 0, "Token", bearerTokenField, true);
        return wrapForm(form);
    }

    private JPanel buildBasicCard() {
        JPanel form = new JPanel(new GridBagLayout());
        addFormRow(form, 0, "Username", basicUsernameField, false);
        addFormRow(form, 1, "Password", basicPasswordField, true);
        return wrapForm(form);
    }

    private JPanel buildApiKeyCard() {
        ButtonGroup group = new ButtonGroup();
        group.add(apiKeyInHeaderRb);
        group.add(apiKeyInQueryRb);

        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioRow.add(apiKeyInHeaderRb);
        radioRow.add(Box.createHorizontalStrut(JBUI.scale(12)));
        radioRow.add(apiKeyInQueryRb);

        JPanel form = new JPanel(new GridBagLayout());
        addFormRow(form, 0, "Key Name", apiKeyNameField,  false);
        addFormRow(form, 1, "Value",    apiKeyValueField,  true);
        addLabeledWidget(form, 2, "Add to", radioRow);
        return wrapForm(form);
    }

    private JPanel buildJwtCard() {
        jwtPayloadArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(12f)));
        jwtPayloadArea.setLineWrap(false);
        JBScrollPane payloadScroll = new JBScrollPane(jwtPayloadArea);

        JPanel form = new JPanel(new GridBagLayout());
        addLabeledWidget(form, 0, "Algorithm", jwtAlgorithmCombo);
        addFormRow(form, 1, "Secret Key", jwtSecretField, true);
        addFormRow(form, 2, "Prefix",     jwtPrefixField, false);
        addLabeledWidget(form, 3, "Payload", payloadScroll);
        return wrapForm(form);
    }

    private JPanel buildDigestCard() {
        JPanel form = new JPanel(new GridBagLayout());
        addFormRow(form, 0, "Username", digestUsernameField, false);
        addFormRow(form, 1, "Password", digestPasswordField, true);
        return wrapForm(form);
    }

    private static JPanel wrapForm(JPanel form) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    // ── Form row helpers ───────────────────────────────────────────────────

    private void addFormRow(JPanel form, int gridy, String labelText, JComponent field, boolean hasEye) {
        GridBagConstraints gbc = rowGbc(gridy);

        gbc.gridx   = 0;
        gbc.weightx = 0;
        gbc.insets  = new Insets(0, 0, JBUI.scale(6), JBUI.scale(8));
        JBLabel lbl = new JBLabel(labelText);
        lbl.setPreferredSize(new Dimension(JBUI.scale(LABEL_WIDTH), lbl.getPreferredSize().height));
        form.add(lbl, gbc);

        gbc.gridx   = 1;
        gbc.weightx = 1.0;
        gbc.insets  = new Insets(0, 0, JBUI.scale(6), hasEye ? JBUI.scale(4) : 0);
        form.add(field, gbc);

        if (hasEye && field instanceof JPasswordField) {
            JPasswordField pf = (JPasswordField) field;
            JToggleButton eyeBtn = new JToggleButton("Show");
            eyeBtn.setMargin(new Insets(1, 6, 1, 6));
            eyeBtn.setFocusable(false);
            eyeBtn.addActionListener(e -> {
                boolean showing = eyeBtn.isSelected();
                pf.setEchoChar(showing ? (char) 0 : '•');
                eyeBtn.setText(showing ? "Hide" : "Show");
            });
            gbc.gridx   = 2;
            gbc.weightx = 0;
            gbc.insets  = new Insets(0, 0, JBUI.scale(6), 0);
            form.add(eyeBtn, gbc);
        }
    }

    private void addLabeledWidget(JPanel form, int gridy, String labelText, JComponent widget) {
        GridBagConstraints gbc = rowGbc(gridy);

        gbc.gridx   = 0;
        gbc.weightx = 0;
        gbc.insets  = new Insets(0, 0, JBUI.scale(6), JBUI.scale(8));
        JBLabel lbl = new JBLabel(labelText);
        lbl.setPreferredSize(new Dimension(JBUI.scale(LABEL_WIDTH), lbl.getPreferredSize().height));
        form.add(lbl, gbc);

        gbc.gridx     = 1;
        gbc.weightx   = 1.0;
        gbc.gridwidth = 2;
        gbc.insets    = new Insets(0, 0, JBUI.scale(6), 0);
        form.add(widget, gbc);
    }

    private static GridBagConstraints rowGbc(int gridy) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy     = gridy;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        return gbc;
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private void notifyChanged() {
        changeListeners.forEach(Runnable::run);
    }

    private static void addDocListener(JTextField field, Runnable onChanged) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onChanged.run(); }
            public void removeUpdate(DocumentEvent e)  { onChanged.run(); }
            public void changedUpdate(DocumentEvent e) { onChanged.run(); }
        });
    }
}
