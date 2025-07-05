package io.apicopilot.codegen.core;

import io.apicopilot.codegen.model.GenerateConfig;
import io.apicopilot.util.JsonUtils;
import io.apicopilot.util.ResourceUtils;
import lombok.Getter;

/**
 * 生成代码配置
 */
public class GenerateConfigs {

    @Getter
    private final GenerateConfig config;
    private static GenerateConfigs instance;

    public static GenerateConfigs getInstance() {
        if (instance == null) {
            instance = new GenerateConfigs();
        }
        return instance;
    }


    public GenerateConfigs() {
        String content = ResourceUtils.readAsText("codegen/config.json");
        this.config = JsonUtils.fromJson(content, GenerateConfig.class);
    }

}
