package io.apicopilot.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源文件工具类
 */
@UtilityClass
public class ResourcesFileUtils {

    private static final Map<String, String> RESOURCES_CACHE = new ConcurrentHashMap<>();

    /**
     * Read resource file as text with cache.
     */
    @SneakyThrows
    public static String readResourceFileAsTextWithCache(String file) {
        return RESOURCES_CACHE.computeIfAbsent(file, ResourcesFileUtils::readResourceFileAsText);
    }

    /**
     * Read resource file as text.
     */
    @SneakyThrows
    public static String readResourceFileAsText(String relativeFile) {
        InputStream is = ResourcesFileUtils.class.getClassLoader().getResourceAsStream(relativeFile);
        if (is == null) {
            throw new FileNotFoundException("Not found file: " + relativeFile);
        }
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
}