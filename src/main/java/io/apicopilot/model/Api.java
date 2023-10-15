package io.apicopilot.model;

import io.apicopilot.document.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Api {

    private final Document document;

    private final Request request;

}
