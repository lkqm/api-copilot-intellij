package io.apix.codegen.context;

import io.apix.codegen.model.PropertyModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ModelContext extends HashMap<String, Object> {

    private String doc;
    private String tag;
    private String modelName;
    private PropertyModel model;
    private List<PropertyModel> models;

}
