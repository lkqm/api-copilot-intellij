package io.apicopilot.document.topic;

import com.intellij.util.messages.Topic;
import io.apicopilot.document.Document;
import io.apicopilot.document.LoadResult;

/**
 * Document topic.
 */
public interface DocumentTopic {
    Topic<DocumentTopic> TOPIC = Topic.create("", DocumentTopic.class);

    default void onAdded(Document document) {
    }

    default void onModified(Document document) {
    }

    default void onDeleted(Document document) {
    }

    default void onLoaded(Document document, LoadResult result) {
    }
}
