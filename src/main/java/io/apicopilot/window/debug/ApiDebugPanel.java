package io.apicopilot.window.debug;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import io.apicopilot.debug.AuthConfig;
import io.apicopilot.debug.DocAuthChangeListener;
import io.apicopilot.debug.DebugHttpClient;
import io.apicopilot.debug.DebugHttpRequest;
import io.apicopilot.debug.DebugHttpResponse;
import io.apicopilot.debug.DebugSettings;
import io.apicopilot.debug.Environment;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Apifox-style debug panel.
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  [GET]  [{{baseUrl}}/users/{id}]           [dev ▼]  [Send]      │  URL bar
 * ├──────────────────────────────────────────────────────────────────┤
 * │  Params │ Authorization │ Headers │ Cookies │ Body               │  tabs
 * ├──────────────────────────────────────────────────────────────────┤
 * │  Response ─────────── [200 OK] · [234ms] · [1.2KB]             │
 * └──────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class ApiDebugPanel extends JPanel implements Disposable {
    private final Project project;
    private final DebugHttpClient httpClient = new DebugHttpClient();

    private Document currentDocument;
    private Request  currentRequest;
    private String   currentPath = "";

    // URL bar
    private final MethodBadge      methodBadge = new MethodBadge();
    private final JBTextField      urlField    = new JBTextField();
    private final EnvDropdownButton envButton  = new EnvDropdownButton();
    private final JButton          sendButton  = new JButton("Send");

    // Request tabs
    private final PathParamPanel  pathParamPanel  = new PathParamPanel();
    private final QueryParamPanel queryParamPanel = new QueryParamPanel();
    private final HeadersPanel    headersPanel    = new HeadersPanel();
    private final CookiesPanel    cookiesPanel    = new CookiesPanel();
    private final AuthPanel       authPanel       = new AuthPanel();
    private final BodyPanel       bodyPanel;
    private final JPanel          paramsPanel     = new JPanel(new BorderLayout());
    private final JPanel          pathSection     = buildParamsSection("Path", pathParamPanel);
    private final JPanel          querySection    = buildParamsSection("Query", queryParamPanel);
    private JBTabbedPane requestTabs;

    // Tab dot labels
    private final DotTabLabel paramsTabLabel  = new DotTabLabel("Params");
    private final DotTabLabel authTabLabel    = new DotTabLabel("Auth");
    private final DotTabLabel headersTabLabel = new DotTabLabel("Headers");
    private final DotTabLabel cookiesTabLabel = new DotTabLabel("Cookies");
    private final DotTabLabel bodyTabLabel    = new DotTabLabel("Body");

    // Center layout — request only until first send, then splitter(request + response)
    private final JPanel    centerPanel           = new JPanel(new BorderLayout());
    private       JBSplitter splitter;
    private       JPanel    requestPanel;
    private       boolean   responsePanelRevealed = false;
    private       boolean   responseCollapsed     = false;
    private       float     savedSplitterProportion = 0.45f;

    // Response
    private final ResponsePanel responsePanel;
    private Future<?> currentRequestTask;
    private long requestSequence = 0;

    public ApiDebugPanel(Project project) {
        super(new BorderLayout());
        this.project = project;

        bodyPanel     = new BodyPanel(project);
        responsePanel = new ResponsePanel(project);
        responsePanel.setCollapseToggleCallback(this::toggleResponseCollapse);

        // Subscribe to document auth changes — auto-unsubscribed when this panel is disposed
        project.getMessageBus().connect(this).subscribe(DocAuthChangeListener.TOPIC,
                (docId, newConfig) -> {
                    if (currentDocument != null && docId.equals(currentDocument.getId())) {
                        authPanel.refreshDocumentAuth(newConfig);
                        authTabLabel.setDotVisible(authPanel.hasValues());
                    }
                });

        add(buildUrlBar(), BorderLayout.NORTH);

        // Initially show only the request panel; response area revealed after first send
        splitter     = new JBSplitter(true, 0.45f);
        requestPanel = buildRequestPanel();
        splitter.setSecondComponent(responsePanel);

        centerPanel.add(requestPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setRequest(Document document, Request request) {
        this.currentDocument = document;
        this.currentRequest  = request;

        methodBadge.setMethod(request.getMethod());
        currentPath = request.getPath() != null ? request.getPath() : "";
        refreshUrlField();

        pathParamPanel.setParams(request.getPath(), request.getParametersIn(ParameterIn.PATH));
        queryParamPanel.setParams(request.getParametersIn(ParameterIn.QUERY));

        boolean hasPathVars = request.getPath() != null && request.getPath().contains("{");
        refreshParamsPanel(hasPathVars);
        paramsTabLabel.setDotVisible(queryParamPanel.hasValues() || pathParamPanel.hasValues());

        List<Parameter> headerParams = request.getParametersIn(ParameterIn.HEADER);
        headersPanel.setSpecHeaders(headerParams);

        cookiesPanel.setSpecCookies(request.getParametersIn(ParameterIn.COOKIE));

        AuthConfig docAuth = DebugSettings.getInstance(project).getAuthConfig(document.getId());
        authPanel.setDocumentAuth(docAuth, () -> openDocAuthDialog());
        authTabLabel.setDotVisible(authPanel.hasValues());

        bodyPanel.setOperation(request.getOperation());
        headersTabLabel.setDotVisible(headersPanel.hasValues());
        bodyTabLabel.setDotVisible(bodyPanel.hasValues());
        if (!queryParamPanel.hasValues() && !hasPathVars && bodyPanel.hasValues()) {
            requestTabs.setSelectedComponent(bodyPanel);
        } else {
            requestTabs.setSelectedComponent(paramsPanel);
        }

        responsePanel.showIdle();
        resetSendButton();
        envButton.repaint();
    }

    /** Rebuilds the URL field from the active environment's baseUrl + current path. */
    public void refreshUrlField() {
        if (currentDocument == null) return;
        Environment activeEnv = DebugSettings.getInstance(project).getActiveEnvironment(currentDocument.getId());
        String baseUrl = (activeEnv != null && activeEnv.getBaseUrl() != null && !activeEnv.getBaseUrl().isEmpty())
                ? activeEnv.getBaseUrl()
                : "";
        urlField.setText(baseUrl + currentPath);
    }

    @Override
    public void dispose() {
        cancelCurrentRequest();
        bodyPanel.dispose();
        responsePanel.dispose();
    }

    // ── URL bar ───────────────────────────────────────────────────────────

    private JPanel buildUrlBar() {
        urlField.getEmptyText().setText("https://api.example.com");
        urlField.setBorder(JBUI.Borders.empty(0, 4));
        urlField.addActionListener(e -> triggerSendAction());

        methodBadge.setBorder(JBUI.Borders.empty(0, 8, 0, 8));

        Color tfBg = UIManager.getColor("TextField.background");
        Color borderColor = UIManager.getColor("Component.borderColor");
        if (borderColor == null) borderColor = UIManager.getColor("Separator.separatorColor");

        JPanel urlComposite = new JPanel(new GridBagLayout());
        urlComposite.setBackground(tfBg);
        urlComposite.setOpaque(true);
        urlComposite.setBorder(BorderFactory.createLineBorder(borderColor, 1));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets  = new Insets(0, 0, 0, 0);

        // [0] Method badge
        gbc.weightx = 0; gbc.gridx = 0;
        urlComposite.add(methodBadge, gbc);

        // [1] URL (fills)
        gbc.weightx = 1.0; gbc.gridx = 1;
        urlComposite.add(urlField, gbc);

        // [2] Separator url|env
        JSeparator envSep = new JSeparator(SwingConstants.VERTICAL);
        envSep.setPreferredSize(new Dimension(1, 16));
        gbc.weightx = 0; gbc.gridx = 2; gbc.fill = GridBagConstraints.VERTICAL;
        urlComposite.add(envSep, gbc);

        // [3] Env button
        gbc.weightx = 0; gbc.gridx = 3; gbc.fill = GridBagConstraints.BOTH;
        urlComposite.add(envButton, gbc);

        // [4] Separator env|send
        JSeparator sendSep = new JSeparator(SwingConstants.VERTICAL);
        sendSep.setPreferredSize(new Dimension(1, 16));
        gbc.weightx = 0; gbc.gridx = 4; gbc.fill = GridBagConstraints.VERTICAL;
        urlComposite.add(sendSep, gbc);

        // [5] Send button
        sendButton.setFocusable(false);
        sendButton.setMargin(new Insets(0, 14, 0, 14));
        sendButton.addActionListener(e -> triggerSendAction());
        gbc.weightx = 0; gbc.gridx = 5; gbc.fill = GridBagConstraints.BOTH;
        urlComposite.add(sendButton, gbc);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(JBUI.Borders.empty(6, 8, 6, 8));
        bar.add(urlComposite, BorderLayout.CENTER);
        return bar;
    }

    // ── Request panel ─────────────────────────────────────────────────────

    private JPanel buildRequestPanel() {
        paramsPanel.setBorder(JBUI.Borders.empty(8));
        refreshParamsPanel(true);

        requestTabs = new JBTabbedPane(JBTabbedPane.TOP);
        requestTabs.addTab("Params",   paramsPanel);    requestTabs.setTabComponentAt(0, paramsTabLabel);
        requestTabs.addTab("Auth",     authPanel);      requestTabs.setTabComponentAt(1, authTabLabel);
        requestTabs.addTab("Headers",  headersPanel);   requestTabs.setTabComponentAt(2, headersTabLabel);
        requestTabs.addTab("Cookies",  cookiesPanel);   requestTabs.setTabComponentAt(3, cookiesTabLabel);
        requestTabs.addTab("Body",     bodyPanel);      requestTabs.setTabComponentAt(4, bodyTabLabel);

        // Dot listeners
        Runnable updateParamsDot = () -> paramsTabLabel.setDotVisible(
                queryParamPanel.hasValues() || pathParamPanel.hasValues());
        queryParamPanel.addValueChangeListener(updateParamsDot);
        pathParamPanel.addValueChangeListener(updateParamsDot);
        authPanel.addValueChangeListener(()    -> authTabLabel.setDotVisible(authPanel.hasValues()));
        headersPanel.addValueChangeListener(() -> headersTabLabel.setDotVisible(headersPanel.hasValues()));
        cookiesPanel.addValueChangeListener(() -> cookiesTabLabel.setDotVisible(cookiesPanel.hasValues()));
        bodyPanel.addValueChangeListener(()   -> bodyTabLabel.setDotVisible(bodyPanel.hasValues()));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(requestTabs, BorderLayout.CENTER);
        return panel;
    }

    private void refreshParamsPanel(boolean showPathSection) {
        paramsPanel.removeAll();
        if (showPathSection) {
            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(pathSection);
            top.add(Box.createVerticalStrut(JBUI.scale(8)));
            paramsPanel.add(top, BorderLayout.NORTH);
        }
        paramsPanel.add(querySection, BorderLayout.CENTER);

        paramsPanel.revalidate();
        paramsPanel.repaint();
    }

    private JPanel buildParamsSection(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        TitledSeparator separator = new TitledSeparator(title);
        separator.setBorder(JBUI.Borders.empty(0, 0, 6, 0));
        panel.add(separator, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ── Doc Auth dialog ───────────────────────────────────────────────────

    private void openDocAuthDialog() {
        if (currentDocument == null) return;
        DebugSettings settings = DebugSettings.getInstance(project);
        AuthConfig current = settings.getAuthConfig(currentDocument.getId());
        // setAuthConfig publishes DocAuthChangeListener.TOPIC — all open panels refresh automatically
        new DocAuthDialog(project, current,
                newConfig -> settings.setAuthConfig(project, currentDocument.getId(), newConfig)
        ).show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String docId() {
        return currentDocument != null ? currentDocument.getId() : "";
    }

    private String resolve(String text) {
        if (currentDocument == null) return text;
        return DebugSettings.getInstance(project).resolve(currentDocument.getId(), text);
    }

    // ── Env dropdown button ───────────────────────────────────────────────

    private void showEnvPopup() {
        JPopupMenu menu = new JPopupMenu();
        DebugSettings settings = DebugSettings.getInstance(project);
        List<Environment> envs = settings.getEnvironments(docId());

        for (Environment env : envs) {
            JMenuItem item = new JMenuItem(env.getName());
            item.addActionListener(e -> {
                settings.setActiveEnvId(docId(), env.getId());
                envButton.repaint();
                refreshUrlField();
            });
            menu.add(item);
        }

        menu.addSeparator();

        if (!settings.getActiveEnvId(docId()).isEmpty()) {
            JMenuItem clearItem = new JMenuItem("\u2716 Clear"); // ✖
            clearItem.addActionListener(e -> {
                settings.setActiveEnvId(docId(), "");
                envButton.repaint();
                refreshUrlField();
            });
            menu.add(clearItem);
        }

        JMenuItem settingsItem = new JMenuItem("\u2699 Settings"); // ⚙
        settingsItem.addActionListener(e -> {
            new ManageEnvDialog(project, docId()).show();
            envButton.repaint();
            refreshUrlField();
        });
        menu.add(settingsItem);

        menu.show(envButton, 0, envButton.getHeight());
    }

    private class EnvDropdownButton extends JButton {

        EnvDropdownButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(JBUI.scale(100), 0));
            addActionListener(e -> showEnvPopup());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth(), h = getHeight();

                // Background
                Color bg = UIManager.getColor("TextField.background");
                if (bg == null) bg = Color.WHITE;
                g2.setColor(bg);
                g2.fillRect(0, 0, w, h);

                // Arrow area
                int arrowAreaW = JBUI.scale(16);
                int arrowW     = JBUI.scale(6);
                int arrowH     = JBUI.scale(3);
                int ax = w - arrowAreaW / 2 - arrowW / 2;
                int ay = (h - arrowH) / 2;
                Color arrowFg = UIManager.getColor("ComboBox.foreground");
                if (arrowFg == null) arrowFg = getForeground();
                g2.setColor(arrowFg);
                g2.fillPolygon(
                        new int[]{ax, ax + arrowW, ax + arrowW / 2},
                        new int[]{ay, ay, ay + arrowH},
                        3);

                // Text
                DebugSettings settings = DebugSettings.getInstance(project);
                Environment active = settings.getActiveEnvironment(docId());

                Font   font;
                Color  fg;
                String text;

                if (active != null) {
                    text = active.getName();
                    fg   = UIManager.getColor("ComboBox.foreground");
                    if (fg == null) fg = getForeground();
                    font = getFont();
                } else {
                    text = "Environment";
                    fg   = UIManager.getColor("TextField.placeholderForeground");
                    if (fg == null) fg = Color.GRAY;
                    font = getFont().deriveFont(Font.ITALIC);
                }

                int textMaxW = w - arrowAreaW - JBUI.scale(8);
                g2.setFont(font);
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                String display = clipText(text, fm, textMaxW);
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(display, JBUI.scale(6), textY);

            } finally {
                g2.dispose();
            }
        }

        private String clipText(String text, FontMetrics fm, int maxWidth) {
            if (fm.stringWidth(text) <= maxWidth) return text;
            String ellipsis = "...";
            int avail = maxWidth - fm.stringWidth(ellipsis);
            int i = text.length();
            while (i > 0 && fm.stringWidth(text.substring(0, i)) > avail) i--;
            return text.substring(0, i) + ellipsis;
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────

    private void triggerSendAction() {
        if (isRequestInFlight()) {
            cancelCurrentRequest();
        } else {
            sendRequest();
        }
    }

    private void sendRequest() {
        if (currentRequest == null || currentDocument == null) return;

        String rawUrl = urlField.getText().trim();
        if (rawUrl.isEmpty()) {
            urlField.requestFocus();
            return;
        }

        DebugHttpRequest req = new DebugHttpRequest();
        req.setMethod(currentRequest.getMethod());
        req.setBaseUrl(resolve(rawUrl));
        req.setPath("");

        // Path params (resolve values)
        Map<String, String> pathParams = new LinkedHashMap<>();
        pathParamPanel.getValues().forEach((k, v) -> pathParams.put(k, resolve(v)));
        req.setPathParams(pathParams);

        // Query params (resolve values, copy to avoid mutating panel state)
        List<DebugHttpRequest.QueryParam> queryParams = new ArrayList<>();
        for (DebugHttpRequest.QueryParam qp : queryParamPanel.getQueryParams()) {
            queryParams.add(new DebugHttpRequest.QueryParam(
                    qp.isEnabled(), qp.getName(), resolve(qp.getValue())));
        }
        req.setQueryParams(queryParams);

        // Headers (resolve values)
        Map<String, String> headers = new LinkedHashMap<>();
        headersPanel.getHeaders().forEach((k, v) -> headers.put(k, resolve(v)));
        String contentType = bodyPanel.getContentType();
        if (contentType != null) headers.put("Content-Type", contentType);

        // Cookies
        String cookieHeader = cookiesPanel.getCookieHeader();
        if (cookieHeader != null) {
            cookieHeader = resolve(cookieHeader);
            String existing = headers.get("Cookie");
            headers.put("Cookie", existing != null ? existing + "; " + cookieHeader : cookieHeader);
        }
        req.setHeaders(headers);

        // Auth: resolve value fields then inject
        AuthConfig authConfig = authPanel.getAuthConfig();
        authConfig.setBearerToken(resolve(authConfig.getBearerToken()));
        authConfig.setBasicUsername(resolve(authConfig.getBasicUsername()));
        authConfig.setBasicPassword(resolve(authConfig.getBasicPassword()));
        authConfig.setApiKeyValue(resolve(authConfig.getApiKeyValue()));
        authConfig.setJwtSecret(resolve(authConfig.getJwtSecret()));
        authConfig.setJwtPayload(resolve(authConfig.getJwtPayload()));
        authConfig.setDigestUsername(resolve(authConfig.getDigestUsername()));
        authConfig.setDigestPassword(resolve(authConfig.getDigestPassword()));
        authConfig.applyTo(req);

        // Body — multipart with files takes priority (must send as bytes)
        byte[] formDataBytes = bodyPanel.getFormDataBodyBytes();
        if (formDataBytes != null) {
            req.setBinaryBody(formDataBytes);
        } else {
            req.setBinaryBody(bodyPanel.getBinaryBody());
            req.setBody(resolve(bodyPanel.getBody()));
        }

        cancelCurrentRequest();
        final long requestId = ++requestSequence;
        showCancelButton();

        // Reveal the response area on the first request
        if (!responsePanelRevealed) {
            centerPanel.remove(requestPanel);
            splitter.setFirstComponent(requestPanel);
            // Allow the divider to be dragged freely without minimum-size constraints
            requestPanel.setMinimumSize(new Dimension(0, 0));
            responsePanel.setMinimumSize(new Dimension(0, 0));
            centerPanel.add(splitter, BorderLayout.CENTER);
            centerPanel.revalidate();
            responsePanelRevealed = true;
        }
        else if (responseCollapsed) {
            splitter.setProportion(savedSplitterProportion);
            responseCollapsed = false;
            responsePanel.updateCollapseButtonState(false);
        }

        responsePanel.showLoading();

        currentRequestTask = AppExecutorUtil.getAppExecutorService().submit(() -> {
            DebugHttpResponse response = httpClient.execute(req);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (requestId != requestSequence) return;
                responsePanel.showResponse(response);
                currentRequestTask = null;
                resetSendButton();
            });
        });
    }

    private boolean isRequestInFlight() {
        return currentRequestTask != null && !currentRequestTask.isDone();
    }

    private void cancelCurrentRequest() {
        Future<?> task = currentRequestTask;
        if (task == null || task.isDone()) {
            currentRequestTask = null;
            return;
        }

        requestSequence++;
        task.cancel(true);
        currentRequestTask = null;

        if (responsePanelRevealed) {
            DebugHttpResponse cancelled = new DebugHttpResponse();
            cancelled.setErrorMessage("Request cancelled");
            responsePanel.showResponse(cancelled);
        }
        resetSendButton();
    }

    private void showCancelButton() {
        sendButton.setEnabled(true);
        sendButton.setText("Cancel");
    }

    private void resetSendButton() {
        sendButton.setEnabled(true);
        sendButton.setText("Send");
    }

    private void toggleResponseCollapse() {
        if (!responsePanelRevealed) return;
        if (responseCollapsed) {
            splitter.setProportion(savedSplitterProportion);
            responseCollapsed = false;
            responsePanel.updateCollapseButtonState(false);
        } else {
            savedSplitterProportion = splitter.getProportion();
            splitter.setProportion(0.95f);
            responseCollapsed = true;
            responsePanel.updateCollapseButtonState(true);
        }
    }
}
