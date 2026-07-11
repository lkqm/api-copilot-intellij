package io.apix.model;

import io.apix.document.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Api {

    private final Document document;

    private final Request request;

}
