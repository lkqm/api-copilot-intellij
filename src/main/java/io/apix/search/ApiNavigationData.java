package io.apix.search;


import lombok.*;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ApiNavigationData {

    /**
     * 文档id
     */
    private String documentId;

    /**
     * 文档id
     */
    private String documentName;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 路径
     */
    private String path;

    /**
     * 概述
     */
    private String summary;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否禁用
     */
    private Boolean deprecated;

}
