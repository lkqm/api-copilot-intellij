package io.apix.model;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.Data;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Request {

    private final String path;
    private final String method;
    private final Operation operation;
    private final OperationExtension extension;

    public Request(@NonNull String path, @NonNull String method, @NonNull Operation operation) {
        this.path = path;
        this.method = method;
        this.operation = operation;
        this.extension = OperationExtension.get(operation);
    }

    public String getId() {
        return extension != null ? extension.getApiId() : null;
    }

    public String getThirdId() {
        return extension != null ? extension.getThirdApiId() : null;
    }

    public List<Parameter> getParametersIn(ParameterIn in) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyList();
        }
        return parameters.stream().filter(parameter -> in.toString().equals(parameter.getIn())).collect(Collectors.toList());
    }

}
