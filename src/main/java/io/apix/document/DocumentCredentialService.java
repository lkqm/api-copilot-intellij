package io.apix.document;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import org.apache.commons.lang3.StringUtils;

/**
 * Stores document secrets in the IDE credential store.
 */
public class DocumentCredentialService {

    public static final String APIFOX_ACCESS_TOKEN = "apifox.accessToken";
    public static final String SWAGGERHUB_API_KEY = "swaggerHub.apiKey";
    private static final String SERVICE_PREFIX = "ApiDocument";

    public static String get(Document document, String key) {
        if (document == null || StringUtils.isEmpty(document.getId())) {
            return null;
        }
        return PasswordSafe.getInstance().getPassword(attributes(document.getId(), key));
    }

    public static void set(Document document, String key, String value) {
        if (document == null || StringUtils.isEmpty(document.getId()) || StringUtils.isBlank(value)) {
            return;
        }
        PasswordSafe.getInstance().set(attributes(document.getId(), key), new Credentials("", value));
    }

    public static void delete(String documentId) {
        if (StringUtils.isEmpty(documentId)) {
            return;
        }
        PasswordSafe.getInstance().set(attributes(documentId, APIFOX_ACCESS_TOKEN), null);
        PasswordSafe.getInstance().set(attributes(documentId, SWAGGERHUB_API_KEY), null);
    }

    private static CredentialAttributes attributes(String documentId, String key) {
        return new CredentialAttributes(SERVICE_PREFIX + "." + documentId + "." + key);
    }
}
