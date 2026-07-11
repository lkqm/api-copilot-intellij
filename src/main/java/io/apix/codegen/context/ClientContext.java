package io.apix.codegen.context;

import io.apix.codegen.model.ApiModel;
import io.apix.codegen.model.PropertyModel;
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
