package apicopilot.model;

import io.swagger.v3.oas.models.Operation;
import lombok.Data;

@Data
public class Request {

    private String method;
    private String path;
    private Operation operation;
    private OperationExtension extension;

    public void setOperation(Operation operation) {
        this.operation = operation;
        this.extension = OperationExtension.get(operation);
    }

    public String getId() {
        return extension != null ? extension.getApiId() : null;
    }

    public String getThirdId() {
        return extension != null ? extension.getThirdApiId() : null;
    }
}
