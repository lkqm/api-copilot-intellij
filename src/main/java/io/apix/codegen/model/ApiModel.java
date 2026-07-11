package io.apix.codegen.model;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API模型
 */
@Data
public class ApiModel {
    private String operationId;
    private String method;
    private String path;
    private List<String> tags;
    private String summary;
    private String description;
    private Boolean deprecated = null;
    private List<PropertyModel> parameters = null;
    private List<PropertyModel> pathParameters = null;
    private List<PropertyModel> queryParameters = null;
    private List<PropertyModel> headerParameters = null;
    private List<PropertyModel> cookieParameters = null;
    private String requestBodyType;
    private PropertyModel requestBody;
    private List<PropertyModel> requestBodyForm;
    private String responseBodyType;
    private PropertyModel responseBody;

    private String queryRequestType;
    private String requestType;
    private String responseType;

    private List<PropertyModel> models;
    private PropertyModel queryModel;
    private List<PropertyModel> requestModels;
    private List<PropertyModel> responseModels;

    public void setParameters(List<PropertyModel> parameters) {
        this.parameters = parameters;
        this.pathParameters = parameters.stream().filter(property -> "path".equals(property.getIn())).collect(Collectors.toList());
        this.queryParameters = parameters.stream().filter(property -> "query".equals(property.getIn())).collect(Collectors.toList());
        this.headerParameters = parameters.stream().filter(property -> "header".equals(property.getIn())).collect(Collectors.toList());
        this.cookieParameters = parameters.stream().filter(property -> "cookie".equals(property.getIn())).collect(Collectors.toList());
    }
}
