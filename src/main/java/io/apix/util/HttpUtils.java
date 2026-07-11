package io.apix.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@UtilityClass
public class HttpUtils {

    /**
     * 下载文件
     */
    public static String downloadText(String url, Duration timeout) throws IOException {
        byte[] bytes = downloadBytes(url, timeout);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 下载文件
     */
    public static byte[] downloadBytes(String url, Duration timeout) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout((int) timeout.toMillis());

        try (InputStream inputStream = connection.getInputStream()) {
            return IOUtils.toByteArray(inputStream);
        }
    }

    /**
     * Post请求
     */
    public byte[] get(@NotNull String url, @Nullable Map<String, String> headers, @NotNull Duration timeout) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout((int) timeout.toMillis());

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        try (InputStream inputStream = connection.getInputStream()) {
            return IOUtils.toByteArray(inputStream);
        }
    }

    /**
     * Post请求
     */
    public byte[] post(@NotNull String url, byte[] body, @Nullable Map<String, String> headers, @NotNull Duration timeout) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout((int) timeout.toMillis());
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // 设置 headers
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        // 写 body
        if (body != null && body.length > 0) {
            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
                outputStream.flush();
            }
        }

        // 读响应
        try (InputStream inputStream = connection.getInputStream()) {
            return IOUtils.toByteArray(inputStream);
        }
    }

}
