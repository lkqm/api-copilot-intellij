package apicopilot.search;


import lombok.*;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ApiNavigationData {

    /**
     * 连接id
     */
    private String connectionId;

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

}
