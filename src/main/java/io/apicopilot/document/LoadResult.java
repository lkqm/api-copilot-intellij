package io.apicopilot.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoadResult {
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
}