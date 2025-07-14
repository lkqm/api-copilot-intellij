package io.apicopilot.codegen.core;

import com.google.common.collect.Lists;
import io.apicopilot.codegen.model.ApiModel;
import io.apicopilot.codegen.model.PropertyModel;
import io.apicopilot.codegen.model.RequestBodyType;
import io.apicopilot.codegen.model.ResponseBodyType;
import io.apicopilot.model.Request;
import io.apicopilot.util.OpenApiUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ApiModelGenerator {

    private final Request request;
    private final String language;
    private final TypeResolver typeResolver;

    public ApiModel get() {
        Operation operation = request.getOperation();
        ApiModel model = new ApiModel();
        model.setOperationId(operation.getOperationId());
        model.setMethod(request.getMethod());
        model.setPath(request.getPath());
        model.setTags(operation.getTags());
        model.setSummary(operation.getSummary());
        model.setDescription(operation.getDescription());
        model.setDeprecated(operation.getDeprecated());
        setParameters(model);
        setRequestBody(model);
        setResponseBody(model);
        setFlatModels(model);
        return model;
    }

    private void setParameters(ApiModel model) {
        Operation operation = request.getOperation();
        if (operation.getParameters() == null || operation.getParameters().isEmpty()) {
            return;
        }

        List<PropertyModel> properties = Lists.newArrayList();
        for (Parameter parameter : operation.getParameters()) {
            PropertyModel property = PropertyModel.of(parameter, typeResolver);
            if (property.getIsObject()) {
                properties.addAll(PropertyModel.ofObject(parameter, typeResolver));
            } else {
                properties.add(property);
            }
        }
        model.setParameters(properties);
    }

    private void setRequestBody(ApiModel model) {
        Operation operation = request.getOperation();
        RequestBody body = operation.getRequestBody();
        if (body == null || body.getContent() == null || body.getContent().isEmpty()) {
            return;
        }
        body.getContent().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getSchema() != null)
                .findFirst().ifPresent(entry -> {
                    String contentType = entry.getKey();
                    MediaType mediaType = entry.getValue();
                    RequestBodyType requestBodyType = RequestBodyType.fromContentType(contentType);
                    PropertyModel requestBody = PropertyModel.of(mediaType.getSchema(), false, typeResolver, "Request");
                    if (requestBodyType != null) {
                        model.setRequestBodyType(requestBodyType.name());
                    }
                    model.setRequestBody(requestBody);
                    if (requestBodyType == RequestBodyType.form || requestBodyType == RequestBodyType.form_data) {
                        model.setRequestBodyForm(requestBody.getProperties());
                    }
                });
    }

    private void setResponseBody(ApiModel model) {
        Operation operation = request.getOperation();
        ApiResponse apiResponse = OpenApiUtils.get2xxResponse(operation.getResponses());
        if (apiResponse == null || apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
            return;
        }
        apiResponse.getContent().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getSchema() != null)
                .findFirst().ifPresent(entry -> {
                    String contentType = entry.getKey();
                    MediaType mediaType = entry.getValue();
                    ResponseBodyType responseBodyType = ResponseBodyType.fromContentType(contentType);
                    PropertyModel responseBody = PropertyModel.of(mediaType.getSchema(), false, typeResolver, "Response");
                    if (responseBodyType != null) {
                        model.setResponseBodyType(responseBodyType.name());
                    }
                    model.setResponseBody(responseBody);
                });
    }

    private void setFlatModels(ApiModel model) {
        model.setModels(Lists.newArrayList());
        PropertyModel requestBody = model.getRequestBody();
        if (requestBody != null) {
            List<PropertyModel> requestModels = Lists.newArrayList();
            if (requestBody.getIsObject()) {
                requestModels.add(requestBody);
            } else if (requestBody.getIsArray() && requestBody.getItems() != null && "object".equals(requestBody.getItems().getType())) {
                requestModels.add(requestBody.getItems());
            }
            requestModels.addAll(requestBody.flatObjectModels());
            model.setRequestModels(requestModels);
            model.getModels().addAll(requestModels);
        }

        PropertyModel responseBody = model.getResponseBody();
        if (responseBody != null) {
            List<PropertyModel> responseBodyModels = Lists.newArrayList();
            if (responseBody.getIsObject()) {
                responseBodyModels.add(responseBody);
            } else if (responseBody.getIsArray() && responseBody.getItems() != null && "object".equals(responseBody.getItems().getType()) && responseBody.getItems().getProperties() != null && !responseBody.getItems().getProperties().isEmpty()) {
                responseBodyModels.add(responseBody.getItems());
            }
            responseBodyModels.addAll(responseBody.flatObjectModels());
            model.setResponseModels(responseBodyModels);
            model.getModels().addAll(responseBodyModels);
        }


    }

}
