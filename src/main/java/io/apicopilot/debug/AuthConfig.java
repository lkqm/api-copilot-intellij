package io.apicopilot.debug;

import lombok.Data;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Authentication configuration for a debug request.
 * Supports No Auth, Bearer Token, Basic Auth, API Key, JWT, and Digest Auth.
 */
@Data
public class AuthConfig {

    public enum Type {
        NONE("No Auth"),
        API_KEY("API Key"),
        BEARER("Bearer Token"),
        JWT("JWT Bearer"),
        BASIC("Basic Auth"),
        DIGEST("Digest Auth");

        private final String displayName;

        Type(String displayName) { this.displayName = displayName; }

        public String getDisplayName() { return displayName; }

        @Override public String toString() { return displayName; }
    }

    private Type type = Type.NONE;

    // Bearer Token
    private String bearerToken = "";

    // Basic Auth
    private String basicUsername = "";
    private String basicPassword = "";

    // API Key
    private String apiKeyName    = "";
    private String apiKeyValue   = "";
    private boolean apiKeyInHeader = true;  // false → query param

    // JWT
    private String jwtAlgorithm = "HS256";   // HS256 | HS384 | HS512
    private String jwtSecret    = "";
    private String jwtPayload   = "{\n  \"sub\": \"1234567890\"\n}";
    private String jwtPrefix    = "Bearer";

    // Digest Auth
    private String digestUsername = "";
    private String digestPassword = "";

    /**
     * Injects auth credentials into the request.
     * - Header-based auth (Bearer, Basic, API Key, JWT): injected via putIfAbsent so user headers win.
     * - Digest: stores credentials on the request; DebugHttpClient handles the challenge-response.
     */
    public void applyTo(DebugHttpRequest req) {
        switch (type) {
            case BEARER:
                if (!bearerToken.trim().isEmpty()) {
                    req.getHeaders().putIfAbsent("Authorization", "Bearer " + bearerToken.trim());
                }
                break;
            case BASIC:
                if (!basicUsername.trim().isEmpty() || !basicPassword.isEmpty()) {
                    String raw = basicUsername.trim() + ":" + basicPassword;
                    String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
                    req.getHeaders().putIfAbsent("Authorization", "Basic " + encoded);
                }
                break;
            case API_KEY:
                String keyName  = apiKeyName.trim();
                String keyValue = apiKeyValue.trim();
                if (!keyName.isEmpty()) {
                    if (apiKeyInHeader) {
                        req.getHeaders().putIfAbsent(keyName, keyValue);
                    } else {
                        req.getQueryParams().add(new DebugHttpRequest.QueryParam(true, keyName, keyValue));
                    }
                }
                break;
            case JWT:
                if (!jwtSecret.isEmpty()) {
                    try {
                        String token  = generateJwt();
                        String prefix = jwtPrefix.trim().isEmpty() ? "Bearer" : jwtPrefix.trim();
                        req.getHeaders().putIfAbsent("Authorization", prefix + " " + token);
                    } catch (Exception ignored) {}
                }
                break;
            case DIGEST:
                req.setDigestUsername(digestUsername);
                req.setDigestPassword(digestPassword);
                break;
            default:
                break;
        }
    }

    // ── JWT generation ─────────────────────────────────────────────────────

    private String generateJwt() throws Exception {
        String headerJson  = "{\"alg\":\"" + jwtAlgorithm + "\",\"typ\":\"JWT\"}";
        String encodedHeader  = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(jwtPayload.getBytes(StandardCharsets.UTF_8));
        String signingInput   = encodedHeader + "." + encodedPayload;

        String macAlgorithm;
        switch (jwtAlgorithm) {
            case "HS384": macAlgorithm = "HmacSHA384"; break;
            case "HS512": macAlgorithm = "HmacSHA512"; break;
            default:      macAlgorithm = "HmacSHA256"; break;
        }
        Mac mac = Mac.getInstance(macAlgorithm);
        mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), macAlgorithm));
        String signature = base64UrlEncode(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));

        return signingInput + "." + signature;
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
