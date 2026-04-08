package io.apicopilot.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-document environment configuration: a list of environments and the active selection.
 */
public class EnvConfig {

    public List<Environment> environments = new ArrayList<>();

    /** ID of the currently active environment; empty string means "No Environment". */
    public String activeEnvId = "";
}
