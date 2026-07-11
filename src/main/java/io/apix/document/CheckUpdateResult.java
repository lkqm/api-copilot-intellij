package io.apix.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckUpdateResult {
    /**
     * 解析是否成功
     */
    private boolean success;

    /**
     * 内容是否变化
     */
    private boolean changed;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 文档内容
     */
    private String content;

    public CheckUpdateResult(boolean success, boolean changed, String failReason) {
        this.success = success;
        this.changed = changed;
        this.failReason = failReason;
    }
}