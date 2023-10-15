package io.apicopilot.model;

import com.google.gson.Gson;
import io.swagger.v3.oas.models.Operation;
import lombok.Data;

/**
 * Operation extends for apilot.
 *
 * @see io.swagger.v3.oas.models.Operation
 */
@Data
public class OperationExtension {

    private static final String KEY = "x-apilot-operation";

    /**
     * 标识id
     */
    private String apiId;

    /**
     * 三方id
     */
    private String thirdApiId;

    public static void set(Operation operation, OperationExtension extension) {
        operation.addExtension(KEY, extension);
    }

    public static OperationExtension get(Operation operation) {
        if (operation == null || operation.getExtensions() == null || operation.getExtensions().isEmpty()) {
            return null;
        }
        Object o = operation.getExtensions().get(KEY);
        if (o == null) {
            return null;
        }

        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(o), OperationExtension.class);
    }
}
