package io.apicopilot.debug;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * A named environment that holds a list of variables.
 */
@Data
public class Environment {
    /** UUID used as a stable key for persistence. */
    private String           id        = "";
    private String           name      = "";
    private String           baseUrl   = "";
    private List<EnvVariable> variables = new ArrayList<>();
}
