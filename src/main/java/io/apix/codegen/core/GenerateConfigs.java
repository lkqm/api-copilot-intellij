package io.apix.codegen.core;

import com.google.common.reflect.TypeToken;
import io.apix.codegen.model.CodeTemplate;
import io.apix.codegen.model.GenerateConfig;
import io.apix.codegen.model.ModelTemplate;
import io.apix.util.JsonUtils;
import io.apix.util.ResourceUtils;
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
