package io.apicopilot.window.debug;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panel for selecting a binary file as request body.
 *
 * <pre>
 * No file:    [Select File]
 * File chosen: report.pdf (24.5 KB)  [×]
 * </pre>
 *
 * Content-Type is detected from the file extension and pushed to the Headers
 * panel via {@link #setOnContentTypeDetected(Consumer)}.
 */
public class BinaryBodyPanel extends JPanel {

    private static final Map<String, String> EXT_TYPES = new LinkedHashMap<>();

    static {
        EXT_TYPES.put("json", "application/json");
        EXT_TYPES.put("xml",  "application/xml");
        EXT_TYPES.put("txt",  "text/plain");
        EXT_TYPES.put("html", "text/html");
        EXT_TYPES.put("csv",  "text/csv");
        EXT_TYPES.put("pdf",  "application/pdf");
        EXT_TYPES.put("zip",  "application/zip");
        EXT_TYPES.put("gz",   "application/gzip");
        EXT_TYPES.put("png",  "image/png");
        EXT_TYPES.put("jpg",  "image/jpeg");
        EXT_TYPES.put("jpeg", "image/jpeg");
        EXT_TYPES.put("gif",  "image/gif");
        EXT_TYPES.put("webp", "image/webp");
        EXT_TYPES.put("mp4",  "video/mp4");
    }

    private final JButton selectBtn = new JButton("Select File");
    private final JLabel  fileLabel = new JLabel();
    private final JButton clearBtn  = new JButton(AllIcons.Actions.Close);

    private byte[]           fileBytes;
    private String           detectedContentType;
    private Consumer<String> onContentTypeDetected;

    public BinaryBodyPanel() {
        super(new BorderLayout());

        // ── File row ──────────────────────────────────────────────────────
        fileLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        clearBtn.setContentAreaFilled(false);
        clearBtn.setBorderPainted(false);
        clearBtn.setFocusable(false);
        clearBtn.setVisible(false);
        clearBtn.addActionListener(e -> clearFile());

        selectBtn.setFocusable(false);
        selectBtn.addActionListener(e -> chooseFile());

        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fileRow.setOpaque(false);
        fileRow.add(selectBtn);
        fileRow.add(fileLabel);
        fileRow.add(clearBtn);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(JBUI.Borders.empty(12, 12));
        center.add(fileRow);

        add(center, BorderLayout.NORTH);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Callback fired with the detected Content-Type when a file is selected. */
    public void setOnContentTypeDetected(Consumer<String> listener) {
        this.onContentTypeDetected = listener;
    }

    /** Returns raw file bytes, or {@code null} if no file chosen. */
    public byte[] getFileBytes() {
        return fileBytes;
    }

    /** Returns the detected Content-Type, or {@code null} if no file chosen. */
    public String getContentType() {
        return detectedContentType;
    }

    public void reset() {
        clearFile();
    }

    // ── Private ───────────────────────────────────────────────────────────

    private void chooseFile() {
        VirtualFile vFile = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withTitle("Select File"),
                null, null);
        if (vFile == null) return;

        File file = VfsUtil.virtualToIoFile(vFile);
        try {
            fileBytes = Files.readAllBytes(file.toPath());
        } catch (IOException ex) {
            Messages.showErrorDialog("Cannot read file: " + ex.getMessage(), "Error");
            return;
        }

        detectedContentType = detectContentType(file);

        // Update UI: hide Select button, show filename + clear
        selectBtn.setVisible(false);
        fileLabel.setText(file.getName() + "  (" + formatSize(fileBytes.length) + ")");
        fileLabel.setForeground(UIManager.getColor("Label.foreground"));
        clearBtn.setVisible(true);

        // Push Content-Type to Headers panel
        if (onContentTypeDetected != null) {
            onContentTypeDetected.accept(detectedContentType);
        }
    }

    private void clearFile() {
        fileBytes           = null;
        detectedContentType = null;

        selectBtn.setVisible(true);
        fileLabel.setText("");
        clearBtn.setVisible(false);
    }

    private static String detectContentType(File file) {
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            String type = EXT_TYPES.get(name.substring(dot + 1));
            if (type != null) return type;
        }
        try {
            String probed = Files.probeContentType(file.toPath());
            if (probed != null) return probed;
        } catch (IOException ignored) {}
        return "application/octet-stream";
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
