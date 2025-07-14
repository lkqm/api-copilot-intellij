package io.apicopilot.util;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.apicopilot.model.Property;
import io.apicopilot.model.Request;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
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
        return getJsonExample(mediaType.getSchema());
    }

    /**
     * 获取参数描述
     */
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
            attaches.add(format("长度: %d~%d", min, max));
        }
        // 数值范围
        if (schema.getMinimum() != null || schema.getMaximum() != null) {
            String min = schema.getMinimum() != null ? schema.getMinimum().toPlainString() : "";
            String max = schema.getMaximum() != null ? schema.getMaximum().toPlainString() : "";
            attaches.add(format("大小: %s~%s", min, max));
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

    private Object doGetJsonExample(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        String type = schema.getType();
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

        MediaType mediaType = apiResponse.getContent().get("application/json");
        if (mediaType == null) {
            return null;
        }
        return getJsonExample(mediaType.getSchema());
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
