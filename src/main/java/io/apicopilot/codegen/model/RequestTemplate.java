package io.apicopilot.codegen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestTemplate {

    /** 分类语言 */
    private String language;

    /** 请求方式名称 */
    private String name;

    /** 模板 */
    private String template;

    public String getTemplate() {
        if(StringUtils.isEmpty(template)){
            String filename = language.toLowerCase() + "_" + name.toLowerCase();
            template = "codegen/request/" + filename + ".hbs";
        }
        return template;
    }
}
