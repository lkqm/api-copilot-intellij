package io.apicopilot.codegen.core;

import com.google.common.reflect.TypeToken;
import io.apicopilot.codegen.model.CodeTemplate;
import io.apicopilot.codegen.model.GenerateConfig;
import io.apicopilot.codegen.model.ModelTemplate;
import io.apicopilot.util.JsonUtils;
import io.apicopilot.util.ResourceUtils;
import lombok.Getter;

import java.util.List;

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
        String code = ResourceUtils.readAsText("codegen/code.json");
        List<CodeTemplate> codeTemplates = JsonUtils.fromJson(code, new TypeToken<List<CodeTemplate>>() {
        }.getType());
        this.config.getCode().setTemplates(codeTemplates);
        String model = ResourceUtils.readAsText("codegen/model.json");
        List<ModelTemplate> modelTemplates = JsonUtils.fromJson(model, new TypeToken<List<ModelTemplate>>() {
        }.getType());
        this.config.getModel().setTemplates(modelTemplates);
    }

}
