package io.apix.document;

import com.intellij.util.xmlb.annotations.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * A global IDE-level connection used to fetch API documents from hosted sources.
 */
@Getter
@Setter
public class Connection {

    private String id;

    private DocumentSourceType type;

    private String name;

    private String baseUrl;

    private ConnectionAuthType authType;

    private String username;

    private String headerName;

    /**
     * Runtime only. Stored in PasswordSafe by {@link ConnectionRepository}.
     * Apifox uses this as an access token; SwaggerHub uses it as an API key.
     */
    @Transient
    private String credential;

    private Long createdAt;

    @Transient
    public String getCredential() {
        return credential;
    }
}
