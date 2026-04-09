package io.apicopilot.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.tabs.UiDecorator;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.util.ui.JBUI;
import io.apicopilot.document.Document;
import io.apicopilot.model.Request;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-tab container on the right side of the tool window, built on {@link JBTabs}.
 *
 * <ul>
 *   <li>{@link #previewOrFocus} — single-click: open/replace the shared preview tab (italic), or focus if already open</li>
 *   <li>{@link #openOrFocus}    — double-click: promote preview → permanent, or open a new permanent tab</li>
 * </ul>
 */
public class ApiViewTabbedPane extends JPanel implements Disposable {

    private static final String CARD_EMPTY = "empty";
    private static final String CARD_TABS  = "tabs";

    private final Project    project;
    private final JBTabs     jbTabs;
    private final CardLayout outerLayout;
    private final Runnable   onAllTabsClosed;

    private final Map<String, TabEntry> tabMap    = new LinkedHashMap<>();
    private       String                previewKey = null;

    private static class TabEntry {
        final String            key;
        final TabInfo           tabInfo;
        final ApiViewDetailPane pane;
        boolean                 isPreview;

        TabEntry(String key, TabInfo tabInfo, ApiViewDetailPane pane, boolean isPreview) {
            this.key       = key;
            this.tabInfo   = tabInfo;
            this.pane      = pane;
            this.isPreview = isPreview;
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────

    public ApiViewTabbedPane(Project project, Runnable onAllTabsClosed) {
        super(new CardLayout());
        this.project         = project;
        this.outerLayout     = (CardLayout) getLayout();
        this.onAllTabsClosed = onAllTabsClosed;

        jbTabs = JBTabsFactory.createTabs(project, this);
        jbTabs.getPresentation()
              .setTabLabelActionsAutoHide(false)
              .setUiDecorator(() -> new UiDecorator.UiDecoration(null, JBUI.insets(6, 10)));

        // Right-click context menu on tabs
        DefaultActionGroup popup = new DefaultActionGroup() {
            @Override public boolean isDumbAware() { return true; }
        };
        popup.add(new AnAction("Close Tab") {
            @Override public boolean isDumbAware() { return true; }
            @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
            @Override public void actionPerformed(@NotNull AnActionEvent e) {
                TabInfo sel = jbTabs.getSelectedInfo();
                if (sel != null) closeTab((String) sel.getObject());
            }
        });
        popup.addSeparator();
        popup.add(new AnAction("Close All Tabs") {
            @Override public boolean isDumbAware() { return true; }
            @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
            @Override public void actionPerformed(@NotNull AnActionEvent e) { closeAllTabs(); }
        });
        popup.add(new AnAction("Close Other Tabs") {
            @Override public boolean isDumbAware() { return true; }
            @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
            @Override public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(tabMap.size() > 1);
            }
            @Override public void actionPerformed(@NotNull AnActionEvent e) {
                TabInfo sel = jbTabs.getSelectedInfo();
                if (sel != null) closeOtherTabs((String) sel.getObject());
            }
        });
        jbTabs.setPopupGroup(popup, ActionPlaces.EDITOR_TAB_POPUP, false);

        // Refresh debug URL on tab switch
        jbTabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSel, TabInfo newSel) {
                if (newSel == null) return;
                TabEntry entry = tabMap.get((String) newSel.getObject());
                if (entry != null) entry.pane.refreshDebugUrl();
            }
        });
        jbTabs.addTabMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                TabInfo clickedInfo = jbTabs.findInfo(e);
                if (clickedInfo == null) return;
                promotePreviewTab((String) clickedInfo.getObject());
            }
        });

        // Empty placeholder shown before any tab is opened
        JLabel emptyLabel = new JLabel("Double-click an API to open it");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        add(emptyLabel,             CARD_EMPTY);
        add(jbTabs.getComponent(), CARD_TABS);
        outerLayout.show(this, CARD_EMPTY);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Single-click: preview tab (italic). Replaces any existing preview tab. */
    public void previewOrFocus(Document document, Request request) {
        String key = tabKey(document, request);
        if (tabMap.containsKey(key) && !key.equals(previewKey)) {
            jbTabs.select(tabMap.get(key).tabInfo, false);
        } else if (key.equals(previewKey)) {
            jbTabs.select(tabMap.get(key).tabInfo, false);
        } else {
            replacePreviewTab(document, request, key);
        }
        outerLayout.show(this, CARD_TABS);
    }

    /** Double-click: permanent tab. Promotes preview if it matches, otherwise opens new tab. */
    public void openOrFocus(Document document, Request request) {
        String key = tabKey(document, request);
        if (tabMap.containsKey(key) && !key.equals(previewKey)) {
            jbTabs.select(tabMap.get(key).tabInfo, false);
        } else if (key.equals(previewKey)) {
            promotePreviewTab();
        } else {
            createPermanentTab(document, request, key);
        }
        outerLayout.show(this, CARD_TABS);
    }

    public Document getCurrentDocument() {
        TabInfo sel = jbTabs.getSelectedInfo();
        if (sel == null) return null;
        TabEntry entry = tabMap.get((String) sel.getObject());
        return entry != null ? entry.pane.getDocument() : null;
    }

    public Request getCurrentRequest() {
        TabInfo sel = jbTabs.getSelectedInfo();
        if (sel == null) return null;
        TabEntry entry = tabMap.get((String) sel.getObject());
        return entry != null ? entry.pane.getRequest() : null;
    }

    public boolean hasOpenTabs() { return !tabMap.isEmpty(); }

    /** Returns the ordered list of open tabs with enough info to restore them. */
    public List<SavedTabState> getTabStates() {
        List<SavedTabState> states = new ArrayList<>();
        TabInfo selectedInfo = jbTabs.getSelectedInfo();
        String selectedKey = selectedInfo != null ? (String) selectedInfo.getObject() : null;
        for (TabEntry entry : tabMap.values()) {
            Document doc = entry.pane.getDocument();
            Request  req = entry.pane.getRequest();
            if (doc == null || doc.getId() == null || req == null) continue;
            states.add(new SavedTabState(
                    doc.getId(), req.getMethod(), req.getPath(),
                    entry.key.equals(selectedKey)));
        }
        return states;
    }

    public static final class SavedTabState {
        public final String  docId;
        public final String  method;
        public final String  path;
        public final boolean selected;

        SavedTabState(String docId, String method, String path, boolean selected) {
            this.docId    = docId;
            this.method   = method;
            this.path     = path;
            this.selected = selected;
        }
    }

    @Override
    public void dispose() {
        for (TabEntry entry : new ArrayList<>(tabMap.values())) {
            entry.pane.dispose();
        }
        tabMap.clear();
    }

    // ── Tab lifecycle ─────────────────────────────────────────────────────

    private void replacePreviewTab(Document document, Request request, String key) {
        int insertAt = jbTabs.getTabCount();
        if (previewKey != null && tabMap.containsKey(previewKey)) {
            TabEntry old = tabMap.get(previewKey);
            insertAt = jbTabs.getIndexOf(old.tabInfo);
            removeTabEntry(previewKey, false);
        }
        previewKey = null;

        ApiViewDetailPane pane = new ApiViewDetailPane(project);
        pane.setRequest(document, request);
        TabInfo tabInfo = buildTabInfo(request, key, true);
        tabInfo.setComponent(pane);

        tabMap.put(key, new TabEntry(key, tabInfo, pane, true));
        jbTabs.addTab(tabInfo, insertAt);
        jbTabs.select(tabInfo, false);
        previewKey = key;
    }

    private void promotePreviewTab() {
        promotePreviewTab(previewKey);
    }

    private void promotePreviewTab(String key) {
        if (key == null || !key.equals(previewKey)) return;
        TabEntry entry = tabMap.get(key);
        previewKey = null;
        if (entry == null) return;
        entry.isPreview = false;
        applyTabText(entry.tabInfo, entry.pane.getRequest(), false);
    }

    private void createPermanentTab(Document document, Request request, String key) {
        ApiViewDetailPane pane = new ApiViewDetailPane(project);
        pane.setRequest(document, request);
        TabInfo tabInfo = buildTabInfo(request, key, false);
        tabInfo.setComponent(pane);

        tabMap.put(key, new TabEntry(key, tabInfo, pane, false));
        jbTabs.addTab(tabInfo);
        jbTabs.select(tabInfo, false);
    }

    private void removeTabEntry(String key, boolean mayShowEmpty) {
        TabEntry entry = tabMap.remove(key);
        if (entry == null) return;
        jbTabs.removeTab(entry.tabInfo);
        if (key.equals(previewKey)) previewKey = null;
        entry.pane.dispose();
        if (mayShowEmpty && tabMap.isEmpty()) {
            outerLayout.show(this, CARD_EMPTY);
            onAllTabsClosed.run();
        }
    }

    private void closeTab(String key) { removeTabEntry(key, true); }

    private void closeAllTabs() {
        for (TabEntry entry : new ArrayList<>(tabMap.values())) {
            jbTabs.removeTab(entry.tabInfo);
            entry.pane.dispose();
        }
        tabMap.clear();
        previewKey = null;
        outerLayout.show(this, CARD_EMPTY);
        onAllTabsClosed.run();
    }

    private void closeOtherTabs(String keepKey) {
        for (TabEntry entry : new ArrayList<>(tabMap.values())) {
            if (!entry.key.equals(keepKey)) {
                jbTabs.removeTab(entry.tabInfo);
                if (entry.key.equals(previewKey)) previewKey = null;
                entry.pane.dispose();
                tabMap.remove(entry.key);
            }
        }
        TabEntry keep = tabMap.get(keepKey);
        if (keep != null) jbTabs.select(keep.tabInfo, false);
    }

    // ── Tab info builder ──────────────────────────────────────────────────

    private TabInfo buildTabInfo(Request request, String key, boolean isPreview) {
        TabInfo tabInfo = new TabInfo(new JPanel());
        tabInfo.setObject(key);
        tabInfo.setIcon(MethodIcon.of(request.getMethod()));
        applyTabText(tabInfo, request, isPreview);

        // Inline close button (auto-hidden on mouse-out, matching IDE editor tab behavior)
        DefaultActionGroup closeGroup = new DefaultActionGroup();
        closeGroup.add(new AnAction((String) null, (String) null, AllIcons.Actions.Close) {
            @Override public boolean isDumbAware() { return true; }
            @Override public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
            @Override public void actionPerformed(@NotNull AnActionEvent e) { closeTab(key); }
        });
        tabInfo.setTabLabelActions(closeGroup, ActionPlaces.EDITOR_TAB);

        return tabInfo;
    }

    private static void applyTabText(TabInfo tabInfo, Request request, boolean isPreview) {
        String label = makeLabel(request);
        tabInfo.clearText(false);
        tabInfo.append(label, isPreview
                ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES
                : SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String makeLabel(Request request) {
        String label = null;
        if (request.getOperation() != null) label = request.getOperation().getSummary();
        if (label == null || label.isEmpty()) label = request.getPath();
        if (label.length() > 22) label = label.substring(0, 20) + "…";
        return label;
    }

    private static String tabKey(Document document, Request request) {
        return document.getId() + "::" + request.getMethod() + "::" + request.getPath();
    }

    // ── Method icon ───────────────────────────────────────────────────────

    /** Renders the HTTP method name in its associated color, used as a tab icon. */
    private static final class MethodIcon implements Icon {

        private final String method;
        private final Color  color;

        static MethodIcon of(String method) {
            String m = method != null ? method.toUpperCase() : "GET";
            return new MethodIcon(m, colorOf(m));
        }

        private MethodIcon(String method, Color color) {
            this.method = method;
            this.color  = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, JBUI.scaleFontSize(10f)));
                g2.setColor(color);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(method);
                int tx = x + (getIconWidth()  - tw)                          / 2;
                int ty = y + (getIconHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(method, tx, ty);
            } finally {
                g2.dispose();
            }
        }

        @Override public int getIconWidth()  { return JBUI.scale(36); }
        @Override public int getIconHeight() { return JBUI.scale(14); }

        private static Color colorOf(String method) {
            switch (method) {
                case "POST":    return new JBColor(new Color(0x0070BB), new Color(0x2196F3));
                case "PUT":     return new JBColor(new Color(0xD97706), new Color(0xFFA726));
                case "PATCH":   return new JBColor(new Color(0x7C3AED), new Color(0xAB47BC));
                case "DELETE":  return new JBColor(new Color(0xDC2626), new Color(0xEF5350));
                case "HEAD":
                case "OPTIONS": return new JBColor(new Color(0x6B7280), new Color(0x9E9E9E));
                default:        return new JBColor(new Color(0x059669), new Color(0x4CAF50)); // GET
            }
        }
    }
}
