package io.apicopilot.codegen.context;

import io.apicopilot.codegen.model.ApiModel;
import io.apicopilot.codegen.model.PropertyModel;
import lombok.*;

import java.util.HashMap;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClientContext extends HashMap<String, Object> {

    private List<PropertyModel> models;
    private List<ApiModel> apis;

}
