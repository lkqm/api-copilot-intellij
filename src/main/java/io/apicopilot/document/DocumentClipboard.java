package io.apicopilot.document;

/**
 * In-memory clipboard for document copy/paste.
 */
public class DocumentClipboard {

    private static Document copied;

    public static void copy(Document document) {
        copied = document;
    }

    public static Document get() {
        return copied;
    }

    public static boolean hasCopy() {
        return copied != null;
    }
}
