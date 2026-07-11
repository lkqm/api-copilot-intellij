package io.apix.codegen.core;

import com.google.common.collect.Lists;
import io.apix.codegen.model.ApiModel;
import io.apix.codegen.model.PropertyModel;
import io.apix.codegen.model.RequestBodyType;
import io.apix.codegen.model.ResponseBodyType;
import io.apix.model.Request;
import io.apix.util.NamedUtils;
import io.apix.util.OpenApiUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class ApiModelGenerator {

    private final Request request;
    private final String language;
    private final TypeResolver typeResolver;

    public ApiModel get() {
        Operation operation = request.getOperation();
        ApiModel api = new ApiModel();
        api.setOperationId(OpenApiUtils.getOrGenerateOperationId(request.getOperation(), request.getPath(), request.getMethod()));
        api.setMethod(request.getMethod());
        api.setPath(request.getPath());
        api.setTags(operation.getTags());
        api.setSummary(operation.getSummary());
        api.setDescription(operation.getDescription());
        api.setDeprecated(operation.getDeprecated());
        setParameters(api);
        setRequestBody(api);
        setResponseBody(api);
        setFlatModels(api);
        return api;
    }

    private void setParameters(ApiModel api) {
        Operation operation = request.getOperation();
        if (operation.getParameters() == null || operation.getParameters().isEmpty()) {
            return;
        }
        String operationId = api.getOperationId();

        List<PropertyModel> properties = Lists.newArrayList();
        for (Parameter parameter : operation.getParameters()) {
            PropertyModel property = PropertyModel.of(parameter, typeResolver);
            if (property.getIsObject()) {
                properties.addAll(PropertyModel.ofObject(parameter, typeResolver));
            } else {
                if (property.getIsArray()) {
                    PropertyModel p = PropertyModel.of(parameter.getSchema().getItems(), false, typeResolver, "");
                    String targetType = property.getTargetType();
                    if (targetType != null) {
                        targetType = String.format(targetType, p.getTargetType());
                    }
                    property.setTargetType(targetType);
                }
                properties.add(property);
            }
        }
        api.setParameters(properties);

        if (CollectionUtils.isNotEmpty(api.getQueryParameters())) {
            String queryType = StringUtils.isEmpty(operationId) ? "QueryRequest" : NamedUtils.toPascalCase(operationId) + "QueryRequest";
            queryType = NamedUtils.toPascalCase(queryType);
            PropertyModel queryModel = new PropertyModel();
            if (StringUtils.isNotEmpty(api.getSummary())) {
                queryModel.setDescription(api.getSummary() + " Query");
            }
            queryModel.setType("object");
            queryModel.setTargetType(queryType);
            queryModel.setIsObject(true);
            queryModel.setIsArray(false);
            queryModel.setProperties(api.getQueryParameters());
            queryModel.setIsModel(true);
            api.setQueryModel(queryModel);
        }
    }

    private void setRequestBody(ApiModel api) {
        Operation operation = request.getOperation();
        RequestBody body = operation.getRequestBody();
        if (body == null || body.getContent() == null || body.getContent().isEmpty()) {
            return;
        }
        String operationId = api.getOperationId();
        String defaultRequestType = StringUtils.isEmpty(operationId) ? "Request" : NamedUtils.toPascalCase(operationId) + "Request";
        body.getContent().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getSchema() != null)
                .findFirst().ifPresent(entry -> {
                    String contentType = entry.getKey();
                    MediaType mediaType = entry.getValue();
                    RequestBodyType requestBodyType = RequestBodyType.fromContentType(contentType);
                    PropertyModel requestBody = PropertyModel.of(mediaType.getSchema(), false, typeResolver, defaultRequestType);
                    if (StringUtils.isEmpty(requestBody.getDescription()) && StringUtils.isNotEmpty(api.getSummary())) {
                        requestBody.setDescription(api.getSummary() + " Request");
                    }
                    if (requestBodyType != null) {
                        api.setRequestBodyType(requestBodyType.name());
                    }
                    api.setRequestBody(requestBody);
                    if (requestBodyType == RequestBodyType.form || requestBodyType == RequestBodyType.form_data) {
                        api.setRequestBodyForm(requestBody.getProperties());
                    }
                });
    }

    private void setResponseBody(ApiModel api) {
        Operation operation = request.getOperation();
        ApiResponse apiResponse = OpenApiUtils.get2xxResponse(operation.getResponses());
        if (apiResponse == null || apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
            return;
        }
        String operationId = api.getOperationId();
        String defaultRequestType = StringUtils.isEmpty(operationId) ? "Response" : NamedUtils.toPascalCase(operationId) + "Response";
        java.util.Map.Entry<String, MediaType> responseEntry = OpenApiUtils.getPreferredContentEntry(apiResponse.getContent());
        if (responseEntry == null || responseEntry.getValue() == null || responseEntry.getValue().getSchema() == null) {
            return;
        }

        String contentType = responseEntry.getKey();
        MediaType mediaType = responseEntry.getValue();
        ResponseBodyType responseBodyType = ResponseBodyType.fromContentType(contentType);
        PropertyModel responseBody = PropertyModel.of(mediaType.getSchema(), false, typeResolver, defaultRequestType);
        if (StringUtils.isEmpty(responseBody.getDescription()) && StringUtils.isNotEmpty(api.getSummary())) {
            responseBody.setDescription(api.getSummary() + " Response");
        }
        if (responseBodyType != null) {
            api.setResponseBodyType(responseBodyType.name());
        }
        responseBody.setIsResponseModel(true);
        api.setResponseBody(responseBody);
    }

    private void setFlatModels(ApiModel api) {
        api.setModels(Lists.newArrayList());
        api.setRequestModels(Lists.newArrayList());
        api.setResponseModels(Lists.newArrayList());
        if (api.getQueryModel() != null) {
            api.getModels().add(api.getQueryModel());
            api.getRequestModels().add(api.getQueryModel());
        }

        PropertyModel requestBody = api.getRequestBody();
        if (requestBody != null) {
            List<PropertyModel> requestModels = Lists.newArrayList();
            if (requestBody.getIsObject() && CollectionUtils.isNotEmpty(requestBody.getProperties())) {
                requestModels.add(requestBody);
            }
            requestModels.addAll(requestBody.flatObjectModels());
            api.getRequestModels().addAll(requestModels);
            api.getModels().addAll(requestModels);
        }

        PropertyModel responseBody = api.getResponseBody();
        if (responseBody != null) {
            List<PropertyModel> responseBodyModels = Lists.newArrayList();
            if (responseBody.getIsObject() && CollectionUtils.isNotEmpty(responseBody.getProperties())) {
                responseBodyModels.add(responseBody);
            }
            responseBodyModels.addAll(responseBody.flatObjectModels());
            api.setResponseModels(responseBodyModels);
            api.getModels().addAll(responseBodyModels);
        }


    }

}
