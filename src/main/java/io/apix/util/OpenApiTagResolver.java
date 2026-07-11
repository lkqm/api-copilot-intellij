package io.apix.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.*;
import java.util.regex.Pattern;

public class OpenApiTagResolver {

    private static final Set<String> IGNORED_PREFIXES = new HashSet<>(Arrays.asList("api", "openapi"));
    private static final Pattern VERSION_PREFIX = Pattern.compile("^v\\d+(\\.\\d+)?$");

    public static String inferResourceName(OpenAPI openApi, String tag) {
        if (!isNonEnglish(tag)) {
            return tag;
        }

        Map<String, Integer> prefixCount = new HashMap<>();

        Map<String, PathItem> paths = openApi.getPaths();
        if (paths == null) return tag;

        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            String rawPath = entry.getKey();
            PathItem pathItem = entry.getValue();

            for (Operation operation : getAllOperations(pathItem)) {
                List<String> tags = operation.getTags();
                if (tags != null && tags.contains(tag)) {
                    String cleanedPrefix = extractResourcePrefix(rawPath);
                    if (!cleanedPrefix.isEmpty()) {
                        prefixCount.put(cleanedPrefix, prefixCount.getOrDefault(cleanedPrefix, 0) + 1);
                    }
                }
            }
        }

        return prefixCount.entrySet()
                .stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(tag);
    }

    private static boolean isNonEnglish(String tag) {
        return !tag.matches("\\p{ASCII}+");
    }

    private static List<Operation> getAllOperations(PathItem pathItem) {
        List<Operation> operations = new ArrayList<>();
        if (pathItem.getGet() != null) operations.add(pathItem.getGet());
        if (pathItem.getPost() != null) operations.add(pathItem.getPost());
        if (pathItem.getPut() != null) operations.add(pathItem.getPut());
        if (pathItem.getDelete() != null) operations.add(pathItem.getDelete());
        if (pathItem.getPatch() != null) operations.add(pathItem.getPatch());
        if (pathItem.getHead() != null) operations.add(pathItem.getHead());
        if (pathItem.getOptions() != null) operations.add(pathItem.getOptions());
        if (pathItem.getTrace() != null) operations.add(pathItem.getTrace());
        return operations;
    }

    private static String extractResourcePrefix(String rawPath) {
        String[] parts = rawPath.split("/");
        List<String> filtered = new ArrayList<>();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            String lower = part.toLowerCase();
            if (IGNORED_PREFIXES.contains(lower) || VERSION_PREFIX.matcher(lower).matches()) {
                continue;
            }
            filtered.add(part);
        }

        return filtered.isEmpty() ? "" : filtered.get(0); // 只取第一个有效段作为资源名
    }
}
