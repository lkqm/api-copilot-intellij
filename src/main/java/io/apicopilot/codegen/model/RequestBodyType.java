package io.apicopilot.codegen.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RequestBodyType {
    form("application/x-www-form-urlencoded"),
    form_data("multipart/form-data"),
    json("application/json"),
    xml("application/xml"),
    binary("application/octet-stream"),
    raw("raw"),
    ;
    private final String contentType;

    public static RequestBodyType fromContentType(String contentType) {
        for (RequestBodyType type : RequestBodyType.values()) {
            if (type.contentType.equals(contentType)) {
                return type;
            }
        }
        return null;
    }
}
