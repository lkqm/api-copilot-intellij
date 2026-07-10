package io.apicopilot.debug;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP client for API debug requests.
 * Supports standard requests and Digest Auth challenge-response (RFC 7616).
 */
public class DebugHttpClient {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public DebugHttpResponse execute(DebugHttpRequest request) {
        DebugHttpResponse response = new DebugHttpResponse();
        long startTime = System.currentTimeMillis();

        try {
            String urlStr = buildUrl(request);
            URI uri = URI.create(urlStr);

            HttpResponse<byte[]> httpResponse = sendRequest(uri, request);

            // Digest Auth: retry on 401 challenge
            if (httpResponse.statusCode() == 401
                    && request.getDigestUsername() != null
                    && !request.getDigestUsername().isEmpty()) {

                String wwwAuth = httpResponse.headers()
                        .firstValue("WWW-Authenticate")
                        .orElse(httpResponse.headers().firstValue("www-authenticate").orElse(null));

                if (wwwAuth != null && wwwAuth.toLowerCase().startsWith("digest ")) {
                    String digestUri = uri.getRawPath()
                            + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                    String authHeader = buildDigestAuthHeader(
                            request.getDigestUsername(), request.getDigestPassword(),
                            request.getMethod(), digestUri, wwwAuth);
                    if (authHeader != null) {
                        request.getHeaders().put("Authorization", authHeader);
                        httpResponse = sendRequest(uri, request);
                    }
                }
            }

            response.setStatusCode(httpResponse.statusCode());

            Map<String, String> headers = new LinkedHashMap<>();
            httpResponse.headers().map().forEach((k, v) -> headers.put(k, String.join(", ", v)));
            response.setHeaders(headers);

            byte[] bytes = httpResponse.body();
            response.setSizeBytes(bytes.length);
            response.setBodyBytes(bytes);

            String contentType = getHeader(headers, "Content-Type");
            String contentDisposition = getHeader(headers, "Content-Disposition");
            response.setContentType(contentType);
            String fileName = fileNameFromContentDisposition(contentDisposition);
            response.setFileName(fileName);
            response.setDownloadFileName(!fileName.isEmpty() ? fileName : defaultFileName(contentType));

            boolean binary = isBinaryResponse(contentType, contentDisposition);
            response.setBinary(binary);
            if (!binary) {
                response.setBody(new String(bytes, StandardCharsets.UTF_8));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setErrorMessage("Request cancelled");
        } catch (Exception e) {
            String msg = e.getMessage();
            response.setErrorMessage(msg != null ? msg : e.getClass().getSimpleName());
        }

        response.setTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }

    // ── Request builder ───────────────────────────────────────────────────

    private HttpResponse<byte[]> sendRequest(URI uri, DebugHttpRequest request) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30));

        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        String method = request.getMethod().toUpperCase();
        HttpRequest.BodyPublisher bodyPublisher;
        if (request.getBinaryBody() != null) {
            bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(request.getBinaryBody());
        } else {
            String bodyStr = request.getBody();
            bodyPublisher = (bodyStr != null && !bodyStr.isEmpty())
                    ? HttpRequest.BodyPublishers.ofString(bodyStr)
                    : HttpRequest.BodyPublishers.noBody();
        }

        switch (method) {
            case "GET":    builder.GET();                          break;
            case "DELETE": builder.DELETE();                       break;
            default:       builder.method(method, bodyPublisher);  break;
        }

        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static String getHeader(Map<String, String> headers, String name) {
        if (headers == null || name == null) return "";
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() != null ? entry.getValue() : "";
            }
        }
        return "";
    }

    private static boolean isBinaryResponse(String contentType, String contentDisposition) {
        String disposition = contentDisposition != null ? contentDisposition.toLowerCase(Locale.ROOT) : "";
        if (disposition.contains("attachment")) {
            return true;
        }

        String mediaType = getMediaType(contentType);
        if (mediaType.isEmpty()) {
            return false;
        }
        if (mediaType.startsWith("text/")) {
            return false;
        }
        if (mediaType.contains("json")
                || mediaType.contains("xml")
                || mediaType.equals("application/javascript")
                || mediaType.equals("application/x-www-form-urlencoded")) {
            return false;
        }
        return mediaType.startsWith("image/")
                || mediaType.equals("application/octet-stream")
                || mediaType.equals("application/pdf")
                || mediaType.equals("application/zip")
                || mediaType.equals("application/x-zip-compressed")
                || mediaType.equals("application/vnd.ms-excel")
                || mediaType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || mediaType.equals("application/msword")
                || mediaType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private static String fileNameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return "";
        }

        Matcher encodedMatcher = Pattern.compile("(?i)(?:^|;)\\s*filename\\*=([^;]+)").matcher(contentDisposition);
        if (encodedMatcher.find()) {
            String decoded = decodeRfc5987FileName(stripQuotes(encodedMatcher.group(1).trim()));
            if (!decoded.isEmpty()) {
                return sanitizeFileName(decoded);
            }
        }

        Matcher matcher = Pattern.compile("(?i)(?:^|;)\\s*filename=([^;]+)").matcher(contentDisposition);
        if (!matcher.find()) {
            return "";
        }
        return sanitizeFileName(stripQuotes(matcher.group(1).trim()));
    }

    private static String decodeRfc5987FileName(String value) {
        int charsetSeparator = value.indexOf("''");
        if (charsetSeparator <= 0 || charsetSeparator + 2 >= value.length()) {
            return value;
        }

        String charsetName = value.substring(0, charsetSeparator);
        String encodedFileName = value.substring(charsetSeparator + 2);
        try {
            return URLDecoder.decode(encodedFileName, Charset.forName(charsetName).name());
        } catch (Exception ignored) {
            return encodedFileName;
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String sanitizeFileName(String value) {
        return value.replace('\\', '_').replace('/', '_');
    }

    private static String defaultFileName(String contentType) {
        return "response" + extensionForContentType(contentType);
    }

    private static String extensionForContentType(String contentType) {
        String mediaType = getMediaType(contentType);
        switch (mediaType) {
            case "application/pdf": return ".pdf";
            case "image/png": return ".png";
            case "image/jpeg": return ".jpg";
            case "image/gif": return ".gif";
            case "application/zip":
            case "application/x-zip-compressed": return ".zip";
            case "application/vnd.ms-excel": return ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": return ".xlsx";
            case "application/msword": return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": return ".docx";
            default: return ".bin";
        }
    }

    private static String getMediaType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return Arrays.stream(contentType.split(";"))
                .findFirst()
                .orElse("")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    // ── URL builder ───────────────────────────────────────────────────────

    private String buildUrl(DebugHttpRequest request) {
        String base = request.getBaseUrl();
        String path = request.getPath();

        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!path.isEmpty() && !path.startsWith("/")) path = "/" + path;

        String fullUrl = base + path;

        // Substitute path params in the full URL
        for (Map.Entry<String, String> entry : request.getPathParams().entrySet()) {
            String encoded = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            fullUrl = fullUrl.replace("{" + entry.getKey() + "}", encoded);
        }

        StringBuilder qs = new StringBuilder();
        for (DebugHttpRequest.QueryParam qp : request.getQueryParams()) {
            if (!qp.isEnabled() || qp.getName() == null || qp.getName().isEmpty()) continue;
            if (qs.length() > 0) qs.append("&");
            qs.append(URLEncoder.encode(qp.getName(), StandardCharsets.UTF_8));
            qs.append("=");
            qs.append(URLEncoder.encode(qp.getValue(), StandardCharsets.UTF_8));
        }

        if (qs.length() > 0) fullUrl += "?" + qs;
        return fullUrl;
    }

    // ── Digest Auth ───────────────────────────────────────────────────────

    /**
     * Builds the Digest Authorization header value per RFC 7616.
     * Supports both qop=auth and legacy (no qop) challenge formats.
     */
    private String buildDigestAuthHeader(String username, String password,
                                         String method, String uri, String wwwAuth) {
        try {
            Map<String, String> challenge = parseDigestChallenge(wwwAuth);
            String realm  = challenge.getOrDefault("realm", "");
            String nonce  = challenge.getOrDefault("nonce", "");
            String qop    = challenge.getOrDefault("qop", "");
            String opaque = challenge.get("opaque");

            String ha1 = md5(username + ":" + realm + ":" + password);
            String ha2 = md5(method.toUpperCase() + ":" + uri);

            String digestResponse;
            StringBuilder sb = new StringBuilder("Digest ");
            sb.append("username=\"").append(username).append("\"");
            sb.append(", realm=\"").append(realm).append("\"");
            sb.append(", nonce=\"").append(nonce).append("\"");
            sb.append(", uri=\"").append(uri).append("\"");

            if (qop.contains("auth")) {
                String nc     = "00000001";
                String cnonce = Long.toHexString(System.currentTimeMillis());
                digestResponse = md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2);
                sb.append(", qop=auth");
                sb.append(", nc=").append(nc);
                sb.append(", cnonce=\"").append(cnonce).append("\"");
            } else {
                digestResponse = md5(ha1 + ":" + nonce + ":" + ha2);
            }

            sb.append(", response=\"").append(digestResponse).append("\"");
            if (opaque != null) sb.append(", opaque=\"").append(opaque).append("\"");

            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses key="value" pairs from a WWW-Authenticate: Digest header. */
    private static Map<String, String> parseDigestChallenge(String wwwAuth) {
        Map<String, String> params = new LinkedHashMap<>();
        // Strip leading "Digest " prefix
        String body = wwwAuth.substring(wwwAuth.indexOf(' ') + 1);
        Matcher m = Pattern.compile("(\\w+)=\"([^\"]*)\"").matcher(body);
        while (m.find()) {
            params.put(m.group(1).toLowerCase(), m.group(2));
        }
        // Also handle unquoted qop values: qop=auth
        Matcher m2 = Pattern.compile("(\\w+)=(\\w+)").matcher(body);
        while (m2.find()) {
            params.putIfAbsent(m2.group(1).toLowerCase(), m2.group(2));
        }
        return params;
    }

    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    // ── JSON formatting ───────────────────────────────────────────────────

    public static String formatJson(String json) {
        if (json == null || json.isEmpty()) return json;
        try {
            JsonElement element = JsonParser.parseString(json);
            return new GsonBuilder().setPrettyPrinting().create().toJson(element);
        } catch (Exception e) {
            return json;
        }
    }
}
