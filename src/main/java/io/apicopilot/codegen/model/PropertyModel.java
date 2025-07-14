package io.apicopilot.codegen.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.apicopilot.codegen.core.TypeResolver;
import io.apicopilot.util.NamedUtils;
import io.apicopilot.util.OpenApiUtils;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字段属性模型
 */
@Data
public class PropertyModel {
    private String name = null;
    private String in = null;
    private String description = null;
    private Boolean required = null;
    private Boolean deprecated = null;
    private String type;
    private String format;
    private String pattern;
    private List<?> enumValues;
    private Object defaultValue;
    private BigDecimal minimum = null;
    private BigDecimal maximum = null;
    private Boolean isArray;
    private Boolean isObject;
    private PropertyModel items;
    private List<PropertyModel> properties;
    private Schema<?> schema;

    /**
     * 代码中的类型
     */
    private String targetType;

    /** 是否新模型 */
    private Boolean isModel;

    /** 数组新模型 */
    private Boolean isArrayModel;

    public String getJsonExample() {
        if (schema == null) {
            return null;
        }
        return OpenApiUtils.getJsonExample(schema);
    }


    public static PropertyModel of(Parameter parameter, TypeResolver typeResolver) {
        Schema<?> schema = parameter.getSchema();

        PropertyModel property = new PropertyModel();
        property.setIn(parameter.getIn());
        property.setName(parameter.getName());
        property.setDescription(parameter.getDescription());
        property.setRequired(parameter.getRequired());

        if (schema != null) {
            property.setSchema(schema);
            property.setType(schema.getType());
            property.setFormat(schema.getFormat());
            property.setPattern(schema.getPattern());
            property.setEnumValues(schema.getEnum());
            property.setDefaultValue(schema.getDefault());
            property.setMinimum(schema.getMinimum());
            property.setMaximum(schema.getMaximum());
            property.setIsArray("array".equals(schema.getType()));
            property.setIsObject("object".equals(schema.getType()));

            property.setTargetType(typeResolver.resolve(schema.getType(), schema.getFormat()));
        }
        return property;
    }

    public static List<PropertyModel> ofObject(Parameter parameter, TypeResolver typeResolver) {
        Schema<?> schema = parameter.getSchema();
        if (!OpenApiUtils.isObjectType(schema)) {
            return Collections.emptyList();
        }

        Set<String> requireds = parameter.getSchema().getRequired() == null ? Collections.emptySet() : Sets.newHashSet(parameter.getSchema().getRequired());
        Map<String, Schema> schemas = schema.getProperties() != null ? schema.getProperties() : Collections.emptyMap();

        return schemas.entrySet().stream().map((entry) -> {
            String name = entry.getKey();
            Schema<?> value = entry.getValue();
            PropertyModel property = of(value, requireds.contains(name), typeResolver, null);
            property.setName(name);
            property.setIn(parameter.getIn());
            property.setRequired(requireds.contains(name));
            return property;
        }).collect(Collectors.toList());
    }

    public static PropertyModel of(Schema<?> schema, Boolean required, TypeResolver typeResolver, String defaultObjectType) {
        PropertyModel property = new PropertyModel();
        property.setSchema(schema);
        property.setDescription(schema.getDescription());
        property.setRequired(required);
        property.setType(schema.getType());
        property.setFormat(schema.getFormat());
        property.setPattern(schema.getPattern());
        property.setEnumValues(schema.getEnum());
        property.setDefaultValue(schema.getDefault());
        property.setMinimum(schema.getMinimum());
        property.setMaximum(schema.getMaximum());
        property.setIsArray("array".equals(schema.getType()));
        property.setIsObject("object".equals(schema.getType()));

        String targetType = typeResolver.resolve(schema.getType(), schema.getFormat());
        Set<String> requires = schema.getRequired() == null ? Collections.emptySet() : Sets.newHashSet(schema.getRequired());
        if ("object".equals(schema.getType()) && schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            String refType = OpenApiUtils.getRefName(schema.get$ref());
            if (StringUtils.isEmpty(refType) && StringUtils.isNotEmpty(defaultObjectType)) {
                refType = defaultObjectType;
            }
            if (StringUtils.isNotEmpty(refType)) {
                refType = NamedUtils.toPascalCase(refType);
                targetType = refType;
            }
            property.setIsModel(true);

            String finalTargetType = targetType;
            List<PropertyModel> properties = schema.getProperties().entrySet().stream()
                    .map((entry) -> {
                        String name = entry.getKey();
                        Schema<?> value = entry.getValue();
                        PropertyModel p = of(value, requires.contains(name), typeResolver, finalTargetType + StringUtils.capitalize(name));
                        p.setName(name);
                        p.setRequired(requires.contains(name));
                        return p;
                    }).collect(Collectors.toList());
            property.setProperties(properties);
        } else if ("array".equals(schema.getType())) {
            PropertyModel p = of(schema.getItems(), false, typeResolver, defaultObjectType);
            property.setItems(p);
            if (targetType != null) {
                targetType = String.format(targetType, p.getTargetType());
            }
            if(BooleanUtils.isTrue(p.isModel)) {
                property.setIsArrayModel(true);
            }
        }
        property.setTargetType(targetType);
        return property;
    }

    public List<PropertyModel> flatObjectModels() {
        List<PropertyModel> objects = Lists.newArrayList();
        if ("object".equals(type) && properties != null && !properties.isEmpty()) {
            for (PropertyModel property : properties) {
                if (property.isObject && property.getProperties() != null && !property.getProperties().isEmpty()) {
                    objects.add(property);
                } else if (property.isArray && property.getItems() != null && property.getItems().isObject) {
                    objects.add(property.getItems());
                }
            }
        } else if ("array".equals(type) && items != null && items.isObject && items.getProperties() != null && !items.getProperties().isEmpty()) {
            objects.add(items);
        }

        List<PropertyModel> children = Lists.newArrayList();
        for (PropertyModel object : objects) {
            children.addAll(object.flatObjectModels());
        }
        objects.addAll(children);
        return objects;
    }
}

