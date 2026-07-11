package io.apix.codegen.context;

import io.apix.codegen.model.PropertyModel;
import lombok.*;

import java.util.HashMap;
import java.util.List;

/**
 * 生成模型的数据上下文.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelsContext extends HashMap<String, Object> {

    private List<PropertyModel> models;

}
