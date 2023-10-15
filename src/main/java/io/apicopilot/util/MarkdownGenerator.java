package io.apicopilot.util;

import io.apicopilot.model.Property;
import io.apicopilot.model.Request;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生成markdown内容
 */
public class MarkdownGenerator {
    public String generateHtml(Request request) {
        String markdown = generate(request);
        return MarkdownUtils.toHtml(markdown);
    }

    public String generate(Request request) {
        String path = request.getPath();
        String method = request.getMethod();
        Operation operation = request.getOperation();

        StringBuilder markdown = new StringBuilder();
        markdown.append(format("### %s", getWithDeprecated(operation.getSummary(), operation.getDeprecated()))).append("\n");
        markdown.append(format("**Path**: %s %s", method, path)).append("\n\n");
        if (StringUtils.isNotEmpty(operation.getDescription())) {
            markdown.append(format("**Description**: %s", operation.getDescription())).append("\n\n");
        }
        markdown.append("**Request**").append("\n\n");
        markdown.append(getParametersSnippets("Headers", request.getParametersIn(ParameterIn.HEADER)));
        markdown.append(getParametersSnippets("Path", request.getParametersIn(ParameterIn.PATH)));
        markdown.append(getParametersSnippets("Query", request.getParametersIn(ParameterIn.QUERY)));
        markdown.append(getRequestBodySnippets("Body", operation.getRequestBody()));
        markdown.append("**Response**").append("\n\n");
        markdown.append(getResponseBodySnippets("Body", operation.getResponses()));
        markdown.append(getResponseBodyDemoSnippets(operation.getResponses()));
        return markdown.toString();
    }

    private String getParametersSnippets(String title, List<Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        parameters = parameters.stream().filter(parameter -> parameter.getSchema() != null).collect(Collectors.toList());
        if (parameters.isEmpty()) {
            return "";
        }
        StringBuilder markdown = new StringBuilder();
        markdown.append(format("*%s:*", title)).append("\n\n");
        markdown.append("| Name | Required | Type | Default | Description |").append("\n");
        markdown.append("| --- | :-: | --- | --- | --- |").append("\n");
        parameters.forEach(parameter -> {
            if (OpenApiUtils.isObjectType(parameter.getSchema())) {
                Property theProperty = new Property(null, parameter.getSchema(), false);
                List<Property> properties = theProperty.getObjectProperties();
                for (Property p : properties) {
                    Schema<?> schema = p.getSchema();
                    String hr = formatTable("| %s | %s | %s | %s | %s |",
                            getWithDeprecated(p.getName(), schema.getDeprecated()),
                            requiredText(p.isRequired()),
                            getSchemaType(schema),
                            schema.getDefault(),
                            OpenApiUtils.getSchemaDescriptionMore(schema));
                    markdown.append(hr).append("\n");
                }
            } else {
                String description = OpenApiUtils.getParameterDescriptionMore(parameter);
                String hr = formatTable("| %s | %s | %s | %s | %s |",
                        getWithDeprecated(parameter.getName(), parameter.getDeprecated()),
                        requiredText(parameter.getRequired()),
                        getSchemaType(parameter.getSchema()),
                        parameter.getSchema().getDefault(),
                        description);
                markdown.append(hr).append("\n");
            }
        });
        markdown.append("\n");
        return markdown.toString();
    }

    private String getRequestBodySnippets(String title, RequestBody body) {
        if (body == null || body.getContent() == null || body.getContent().isEmpty()) {
            return "";
        }
        StringBuilder markdown = new StringBuilder();
        markdown.append(format("*%s:*", title)).append("\n\n");
        body.getContent().forEach((contentType, mediaType) -> {
            markdown.append(format("%s", contentType)).append("\n\n");
            markdown.append(doGetSchemaTableSnippets(mediaType.getSchema())).append("\n\n");
        });
        return markdown.toString();
    }

    private String getResponseBodySnippets(String title, ApiResponses responses) {
        ApiResponse apiResponse = OpenApiUtils.get2xxResponse(responses);
        if (apiResponse == null || apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
            return "";
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append(format("*%s:*", title)).append("\n\n");
        apiResponse.getContent().forEach((contentType, mediaType) -> {
            if ("*/*".equals(contentType)) {
                contentType = "application/json";
            }
            markdown.append(format("%s", contentType)).append("\n\n");
            markdown.append(doGetSchemaTableSnippets(mediaType.getSchema())).append("\n\n");
        });
        return markdown.toString();
    }

    private String getResponseBodyDemoSnippets(ApiResponses responses) {
        ApiResponse apiResponse = OpenApiUtils.get2xxResponse(responses);
        if (apiResponse == null || apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
            return "";
        }
        MediaType mediaType = apiResponse.getContent().get("application/json");
        if (mediaType == null) {
            mediaType = apiResponse.getContent().get("*/*");
        }
        if (mediaType == null) {
            return "";
        }

        String jsonExample = OpenApiUtils.getJsonExample(mediaType.getSchema());
        StringBuilder markdown = new StringBuilder();
        markdown.append("```json").append("\n");
        markdown.append(jsonExample).append("\n");
        markdown.append("```\n");
        return markdown.toString();
    }

    private String doGetSchemaTableSnippets(Schema<?> schema) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("| Name | Required | Type | Default | Description |").append("\n");
        markdown.append("| --- | :-: | --- | --- | --- |").append("\n");
        if ("object".equals(schema.getType())) {
            Property schemaWrapper = new Property(null, schema, false);
            List<Property> properties = schemaWrapper.getObjectProperties();
            for (int i = 0; i < properties.size(); i++) {
                Property p = properties.get(i);
                markdown.append(propertyRowSnippets(p, 1, i == properties.size() - 1));
            }
        } else if ("array".equals(schema.getType())) {
            Property property = new Property("", schema.getItems(), false);
            markdown.append(propertyRowSnippets(property, 1, true));
        } else {
            Property property = new Property("", schema, false);
            markdown.append(propertyRowSnippets(property, 1, true));
        }
        return markdown.toString();
    }


    private String propertyRowSnippets(Property property, int depth, boolean isLast) {
        String nameDepth = getWithDeprecated(property.getName(), property.getSchema().getDeprecated());
        String tree = isLast ? "└ " : "├ ";
        if (depth > 1 && StringUtils.isNotEmpty(property.getName())) {
            nameDepth = StringUtils.repeat("&nbsp;&nbsp;", depth - 1) + tree + getWithDeprecated(property.getName(), property.getSchema().getDeprecated());
        }
        Schema<?> schema = property.getSchema();
        String row = formatTable("| %s | %s | %s | %s | %s |\n",
                nameDepth, requiredText(property.isRequired()), getSchemaType(schema), schema.getDefault(), OpenApiUtils.getSchemaDescriptionMore(schema));
        StringBuilder markdown = new StringBuilder(row);

        // 对象或对象数组
        List<Property> properties = null;
        if (property.isObjectType()) {
            properties = property.getObjectProperties();
        } else if (property.isArrayObjectType()) {
            properties = property.getArrayObjectProperties();
        }
        if (properties != null) {
            for (int i = 0; i < properties.size(); i++) {
                Property p = properties.get(i);
                markdown.append(propertyRowSnippets(p, depth + 1, i == properties.size() - 1));
            }
        }

        // 多维普通数组
        boolean multipleArray = property.isMultipleArrayType();
        if (multipleArray) {
            Property nextProperty = new Property(null, schema.getItems(), false);
            markdown.append(propertyRowSnippets(nextProperty, depth + 1, isLast));
        }
        return markdown.toString();
    }


    //----------------------- 辅助方法 ---------------------------//

    private String getSchemaType(Schema<?> schema) {
        String type = schema.getType();
        String format = schema.getFormat();
        if ("array".equals(schema.getType()) && schema.getItems() != null) {
            type = schema.getItems().getType() + "[]";
            format = schema.getItems().getFormat();
        }
        if (StringUtils.isNotEmpty(format)) {
            type = String.format("%s *&lt;%s&gt;*", type, format);
        }
        return type;
    }

    private String getWithDeprecated(String text, Boolean deprecated) {
        return BooleanUtils.isTrue(deprecated) ? "<del>" + text + "</del>" : text;
    }

    private String format(String format, Object... values) {
        Object[] objects = Arrays.stream(values).map(v -> v == null ? "" : v).toArray();
        return String.format(format, objects);
    }

    private String formatTable(String format, Object... values) {
        Object[] objects = Arrays.stream(values).map(v -> v == null ? "" : escapeTable(v.toString())).toArray();
        return String.format(format, objects);
    }

    private String requiredText(Boolean required) {
        return Boolean.TRUE.equals(required) ? "✓" : "-";
    }

    private String escapeTable(String value) {
        return value.replaceAll("\\|", "\\\\|");
    }

}