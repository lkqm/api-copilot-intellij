package io.apix.debug;

import com.intellij.util.messages.Topic;

/**
 * Message bus topic published whenever the document-level auth config is saved.
 * All open {@code ApiDebugPanel} instances subscribe to this topic so they can
 * refresh their auth preview without any direct coupling.
 */
public interface DocAuthChangeListener {

    Topic<DocAuthChangeListener> TOPIC = Topic.create(
            "Apix.DocAuthChanged", DocAuthChangeListener.class
    );

    /**
     * @param docId     the document whose auth config changed
     * @param newConfig the newly saved auth config
     */
    void docAuthChanged(String docId, AuthConfig newConfig);
}
