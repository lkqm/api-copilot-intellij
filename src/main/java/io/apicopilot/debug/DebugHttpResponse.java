package io.apicopilot.debug;

import lombok.Data;

import java.util.Map;

/**
 * Debug HTTP response model.
 */
@Data
public class DebugHttpResponse {

    private int statusCode;
    private Map<String, String> headers;
    private String body;
    private long timeMs;
    private long sizeBytes;
    private String errorMessage;

    public boolean isSuccess() {
        return errorMessage == null;
    }

    /** e.g. "200 OK" */
    public String getStatusText() {
        return statusCode + " " + getHttpStatusReason(statusCode);
    }

    /** e.g. "128 ms" */
    public String getTimeText() {
        return timeMs + " ms";
    }

    /** e.g. "1.24 KB" */
    public String getSizeText() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
    }

    public boolean isJsonResponse() {
        if (headers == null) return false;
        String ct = headers.getOrDefault("content-type", "");
        return ct.contains("application/json") || ct.contains("text/json");
    }

    private static String getHttpStatusReason(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 422: return "Unprocessable Entity";
            case 429: return "Too Many Requests";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "";
        }
    }
}
