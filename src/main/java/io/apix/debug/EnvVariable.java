package io.apix.debug;

import lombok.Data;

/**
 * A single environment variable (key-value pair with enabled flag).
 */
@Data
public class EnvVariable {
    private boolean enabled = true;
    private String  name    = "";
    private String  value   = "";
}
