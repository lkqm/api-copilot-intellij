package io.apix.codegen.core;

import com.google.common.reflect.TypeToken;
import io.apix.codegen.model.TypeMapping;
import io.apix.util.JsonUtils;
import io.apix.util.ResourceUtils;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class TypeMappings {

    private final Map<String, List<TypeMapping>> typeMappings;
    private final Map<String, List<TypeMapping>> defaultTypeMappings;
    public static TypeMappings instance;

    public static TypeMappings getInstance() {
        if (instance == null) {
            instance = new TypeMappings();
        }
        return instance;
    }


    public TypeMappings() {
        String types = ResourceUtils.readAsTextWithCache("codegen/types.json");
        this.defaultTypeMappings = JsonUtils.fromJson(types, new TypeToken<Map<String, List<TypeMapping>>>() {
        }.getType());
        this.typeMappings = new HashMap<>(defaultTypeMappings);
    }

    public List<TypeMapping> get(@NotNull String language) {
        return typeMappings.getOrDefault(language, Collections.emptyList());
    }

}
