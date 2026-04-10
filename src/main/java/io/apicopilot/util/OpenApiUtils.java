package io.apicopilot.util;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.apicopilot.model.Property;
import io.apicopilot.model.Request;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

@UtilityClass
public class OpenApiUtils {

    /**
     * 获取接口数量
     */
    public int countApi(OpenAPI openApi) {
        if (openApi == null) {
            return 0;
        }
        AtomicInteger count = new AtomicInteger();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                count.getAndIncrement();
            });
        });
        return count.get();
    }

    /**
     * 获取接口数量
     */
    public int countApiByTag(OpenAPI openApi, String tag) {
        if (openApi == null) {
            return 0;
        }
        AtomicInteger count = new AtomicInteger();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                if (operation.getTags().contains(tag)) {
                    count.getAndIncrement();
                }
            });
        });
        return count.get();
    }

    /**
     * 构建请求json
     */
    public String buildRequestJson(Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) {
            return "";
        }
        MediaType mediaType = requestBody.getContent().get("application/json");
        if (mediaType == null || mediaType.getSchema() == null) {
            return "";
        }
        String example = getExampleText(mediaType, "application/json");
        return example != null ? example : "";
    }

    /**
     * 获取参数描述
     */
    /**
     * Returns the human-readable type string for a schema, e.g. {@code integer <int64>}, {@code string[]}.
     * Consistent with the type chip display in the document preview pane.
     */
    public static String schemaTypeDisplay(Schema<?> schema) {
        if (schema == null) return "string";
        String type = schema.getType();
        if ("array".equals(type) && schema.getItems() != null) {
            String itemType = schema.getItems().getType();
            return (itemType != null ? itemType : "object") + "[]";
        }
        if (type == null) return "object";
        String fmt = schema.getFormat();
        return (fmt != null && !fmt.isEmpty()) ? type + " <" + fmt + ">" : type;
    }

    public String getParameterDescriptionMore(Parameter parameter) {
        String description = parameter.getDescription() != null ? parameter.getDescription() : "";
        if (parameter.getSchema() == null) {
            return description;
        }
        return doGetAttachDescription(description, parameter.getSchema());
    }

    /**
     * 获取schema描述
     */
    public String getSchemaDescriptionMore(Schema<?> schema) {
        String description = schema.getDescription() != null ? schema.getDescription() : "";
        return doGetAttachDescription(description, schema);
    }

    private String doGetAttachDescription(String description, Schema<?> schema) {
        List<String> attaches = Lists.newArrayList();
        // 长度范围
        if (schema.getMinLength() != null || schema.getMaxLength() != null) {
            int min = schema.getMinLength() != null ? schema.getMinLength() : 0;
            int max = schema.getMaxLength() != null ? schema.getMaxLength() : Integer.MAX_VALUE;
            attaches.add(format("length: %d~%d", min, max));
        }
        // 数值范围
        if (schema.getMinimum() != null || schema.getMaximum() != null) {
            String min = schema.getMinimum() != null ? schema.getMinimum().toPlainString() : "";
            String max = schema.getMaximum() != null ? schema.getMaximum().toPlainString() : "";
            attaches.add(format("range: %s~%s", min, max));
        }
        // 数组长度
        if (schema.getMinItems() != null || schema.getMaxItems() != null) {
            String min = schema.getMinItems() != null ? String.valueOf(schema.getMinItems()) : "0";
            String max = schema.getMaxItems() != null ? String.valueOf(schema.getMaxItems()) : "∞";
            attaches.add(format("items: %s~%s", min, max));
        }
        // 默认值
        if (schema.getDefault() != null) {
            attaches.add("default: " + schema.getDefault());
        }

        if (!attaches.isEmpty()) {
            description += " [" + String.join(", ", attaches) + "]";
        }
        return description;
    }

    /**
     * 获取json示例
     */
    public String getJsonExample(Schema<?> schema) {
        Object example = doGetJsonExample(schema);
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        return gson.toJson(example);
    }

    public String getExampleText(MediaType mediaType, String contentType) {
        if (mediaType == null) {
            return null;
        }

        Object mediaExample = mediaType.getExample();
        if (mediaExample != null) {
            return stringifyExampleValue(mediaExample, contentType);
        }

        Map<String, Example> examples = mediaType.getExamples();
        if (examples != null) {
            for (Example example : examples.values()) {
                if (example == null) continue;
                if (example.getValue() != null) {
                    return stringifyExampleValue(example.getValue(), contentType);
                }
                if (StringUtils.isNotBlank(example.getExternalValue())) {
                    return example.getExternalValue();
                }
            }
        }

        return getExampleText(mediaType.getSchema(), contentType);
    }

    public String getExampleText(Schema<?> schema, String contentType) {
        if (schema == null) {
            return null;
        }
        if (schema.getExample() != null) {
            return stringifyExampleValue(schema.getExample(), contentType);
        }
        if (schema.getDefault() != null) {
            return stringifyExampleValue(schema.getDefault(), contentType);
        }
        List<?> enumValues = schema.getEnum();
        if (enumValues != null && !enumValues.isEmpty()) {
            return stringifyExampleValue(enumValues.get(0), contentType);
        }
        return getJsonExample(schema);
    }

    private String stringifyExampleValue(Object value, String contentType) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) {
            return gson.toJson(value);
        }
        return String.valueOf(value);
    }

    private Object doGetJsonExample(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        String type = schema.getType();
        if(StringUtils.isEmpty(type)) {
            return null;
        }
        String format = schema.getFormat();
        switch (type) {
            case "boolean":
                return true;
            case "integer":
                return 1;
            case "number":
                return 1.0;
            case "string":
                if ("date".equals(format)) {
                    return DateFormatUtils.format(new Date(), "yyyy-MM-dd");
                } else if ("date-time".equals(format)) {
                    return DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
                } else if ("time".equals(format)) {
                    return DateFormatUtils.format(new Date(), "HH:mm:ss");
                } else {
                    return "";
                }
        }

        Property property = new Property(null, schema, false);
        if (property.isObjectType()) {
            Map<String, Object> data = new LinkedHashMap<>();
            List<Property> properties = property.getObjectProperties();
            properties.forEach(p -> {
                data.put(p.getName(), doGetJsonExample(p.getSchema()));
            });
            return data;
        }
        if (property.isArrayType()) {
            List<Object> data = new ArrayList<>();
            data.add(doGetJsonExample(property.getSchema().getItems()));
            return data;
        }

        return new Object();
    }

    /**
     * 是否有请求体
     */
    public boolean hasRequestBody(Operation operation) {
        return operation.getRequestBody() != null && operation.getRequestBody().getContent() != null
                && !operation.getRequestBody().getContent().isEmpty();
    }

    /**
     * 是否有响应体
     */
    public boolean hasResponseBody(Operation operation) {
        return operation.getResponses() != null && !operation.getResponses().isEmpty();
    }

    public String buildResponseJson(ApiResponses responses) {
        ApiResponse apiResponse = get2xxResponse(responses);
        if (apiResponse == null) {
            return null;
        }

        Map.Entry<String, MediaType> contentEntry = getPreferredContentEntry(apiResponse.getContent());
        if (contentEntry == null) {
            return null;
        }
        return getExampleText(contentEntry.getValue(), contentEntry.getKey());
    }

    /**
     * 获取成功响应，优先级200 > 201 > 202
     */
    public ApiResponse get2xxResponse(ApiResponses responses) {
        if (responses == null || responses.isEmpty()) {
            return null;
        }

        ApiResponse apiResponse = responses.get("200");
        if (apiResponse == null) {
            apiResponse = responses.get("201");
        }
        if (apiResponse == null) {
            apiResponse = responses.get("202");
        }
        return apiResponse;
    }

    public Map.Entry<String, MediaType> getPreferredContentEntry(Map<String, MediaType> content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        Map.Entry<String, MediaType> exactJson = findContentEntry(content, "application/json", true);
        if (exactJson != null) {
            return exactJson;
        }

        Map.Entry<String, MediaType> structuredJson = content.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().getSchema() != null)
                .filter(entry -> {
                    String key = entry.getKey();
                    return key != null && key.endsWith("+json");
                })
                .findFirst()
                .orElse(null);
        if (structuredJson != null) {
            return structuredJson;
        }

        Map.Entry<String, MediaType> firstTyped = content.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().getSchema() != null)
                .filter(entry -> {
                    String key = entry.getKey();
                    return key != null && !key.equals("*/*") && !key.endsWith("/*");
                })
                .findFirst()
                .orElse(null);
        if (firstTyped != null) {
            return firstTyped;
        }

        Map.Entry<String, MediaType> wildcardJson = findContentEntry(content, "*/*", true);
        if (wildcardJson != null) {
            return wildcardJson;
        }

        return content.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .findFirst()
                .orElse(null);
    }

    private Map.Entry<String, MediaType> findContentEntry(Map<String, MediaType> content, String contentType, boolean requireSchema) {
        return content.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getKey(), contentType))
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !requireSchema || entry.getValue().getSchema() != null)
                .findFirst()
                .orElse(null);
    }

    public boolean isObjectType(Schema<?> schema) {
        return schema != null && "object".equals(schema.getType());
    }

    public boolean isArrayType(Schema<?> schema) {
        return schema != null && "array".equals(schema.getType());
    }

    public String getRefName(String ref) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        if (ref.contains("/properties/")) {
            String[] split = ref.split("/");
            String refSchema = split[3];
            ref = ref.substring(ref.lastIndexOf("/") + 1);
        } else {
            ref = ref.substring(ref.lastIndexOf("/") + 1);
        }
        return ref;
    }

    public List<Request> getRequests(OpenAPI openApi) {
        List<Request> requests = new ArrayList<>();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                Request request = new Request(path, method.name(), operation);
                requests.add(request);
            });
        });
        return requests;
    }

    public String getOrGenerateOperationId(Operation operation, String path, String httpMethod) {
        if (StringUtils.isNotEmpty(operation.getOperationId())) {
            return operation.getOperationId();
        }
        String tmpPath = path.replaceAll("\\{", "").replaceAll("\\}", "");
        if (tmpPath.equals("/")) {
            return httpMethod.toLowerCase() + "Root";
        }
        tmpPath = httpMethod.toLowerCase() + tmpPath;
        // 将路径分隔符替换为空格，以便 NamedUtils.toCamelCase 能正确处理
        tmpPath = tmpPath.replace("/", " ");
        return NamedUtils.toCamelCase(tmpPath);
    }
}
