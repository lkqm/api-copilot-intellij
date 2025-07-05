package io.apicopilot.codegen.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseBodyType {
    json("application/json"),
    binary("application/octet-stream"),
    ;
    private final String contentType;

    public static ResponseBodyType fromContentType(String contentType) {
        for (ResponseBodyType type : values()) {
            if (type.contentType.equals(contentType)) {
                return type;
            }
        }
        return null;
    }

}
