package apicopilot.window.tree;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;


public interface ApiTreeTopic {

    Topic<ApiTreeTopic> TOPIC = Topic.create("ApiTreeTopic", ApiTreeTopic.class);


    void action(@NotNull ApiTree.RenderArgs data);
}
