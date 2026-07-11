package io.apix.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import io.apix.document.Document;
import io.apix.window.support.PreviewState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * API view.
 */
public interface ApiView {

    static ApiView getInstance(@NotNull Project project) {
        return project.getService(ApiView.class);
    }

    /**
     * 初始化设置
     */
    void setup(ToolWindow toolWindow);


    /**
     * 选中特定api节点
     */
    void select(String documentId, String path, String method);


    /**
     * 刷新视图
     */
    void refresh();

    /**
     * 显示详情面板
     */
    PreviewState switchPreviewState();

    /**
     * 获取选中的文档节点
     */
    List<Document> getSelectedDocuments();

    /**
     * 删除文档
     */
    void deleteDocuments(List<Document> documents);

    /**
     * 定位文档
     */
    void locate();

    ApiViewPanel getPanel();
}
