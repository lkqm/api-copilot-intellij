package io.apix.document.resolver;

import org.jetbrains.annotations.NotNull;

/**
 * Resolve API content.
 */
public interface ApiResolver {

    @NotNull
    ResolveResult resolve(boolean refresh);


}
