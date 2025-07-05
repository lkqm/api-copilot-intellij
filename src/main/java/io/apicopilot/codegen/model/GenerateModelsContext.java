package io.apicopilot.codegen.model;

import lombok.*;

import java.util.HashMap;
import java.util.List;

/**
 * 生成模型的数据上下文.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateModelsContext {

    private List<PropertyModel> models;

}
