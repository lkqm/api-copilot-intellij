package io.apix.document;

/**
 * 文档同步状态.
 */
public enum SyncStatus {
    /**
     * 初始状态，未同步过
     */
    IDLE,
    /**
     * 同步中
     */
    SYNCING,
    /**
     * 最近一次同步成功
     */
    SUCCESS,
    /**
     * 最近一次同步失败
     */
    FAILED
}
