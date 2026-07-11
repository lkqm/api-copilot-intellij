package io.apix.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import io.apix.util.LanguageUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CodegenLanguageResolver {

    private CodegenLanguageResolver() {}

    public static String resolveRequestLanguage(Project project, List<String> supports) {
        String saved = project != null ? CodegenSettings.getInstance(project).requestLastLanguage : null;
        String resolved = firstSupported(supports, saved, inferProjectLanguage(project, supports, true));
        if (resolved != null) {
            return resolved;
        }
        String fallback = LanguageUtils.getDefaultLanguage(supports, "C#");
        return normalizeRequestLanguage(fallback, supports);
    }

    public static String resolveModelLanguage(Project project, List<String> supports) {
        String saved = project != null ? CodegenSettings.getInstance(project).modelLastLanguage : null;
        String resolved = firstSupported(supports, saved, inferProjectLanguage(project, supports, false));
        if (resolved != null) {
            return resolved;
        }
        return LanguageUtils.getDefaultLanguage(supports, "C#");
    }

    private static String inferProjectLanguage(Project project, List<String> supports, boolean requestCode) {
        if (project == null || supports == null || supports.isEmpty()) {
            return null;
        }

        Set<String> candidates = new LinkedHashSet<>();
        addByMarkerFile(project, "tsconfig.json", candidates, "TypeScript", "JavaScript");
        addByMarkerFile(project, "package.json", candidates, "TypeScript", "JavaScript");
        addByMarkerFile(project, "vite.config.ts", candidates, "TypeScript", "JavaScript");
        addByMarkerFile(project, "vite.config.js", candidates, "JavaScript", "TypeScript");
        addByMarkerFile(project, "pom.xml", candidates, "Java", "Kotlin");
        addByMarkerFile(project, "build.gradle", candidates, "Java", "Kotlin");
        addByMarkerFile(project, "build.gradle.kts", candidates, "Kotlin", "Java");
        addByMarkerFile(project, "settings.gradle.kts", candidates, "Kotlin", "Java");
        addByMarkerFile(project, "go.mod", candidates, "Go");
        addByMarkerFile(project, "pyproject.toml", candidates, "Python");
        addByMarkerFile(project, "requirements.txt", candidates, "Python");
        addByMarkerFile(project, "composer.json", candidates, "PHP");
        addByMarkerFile(project, "Gemfile", candidates, "Ruby");
        addByMarkerFile(project, "Cargo.toml", candidates, "Rust");
        addByMarkerFile(project, ".csproj", candidates, "C#");
        addByMarkerFile(project, ".sln", candidates, "C#");

        if (requestCode) {
            candidates.add("JavaScript");
            candidates.add("TypeScript");
        }

        List<String> ordered = new ArrayList<>();
        for (String candidate : candidates) {
            if (requestCode) {
                candidate = normalizeRequestLanguage(candidate, supports);
            }
            if (StringUtils.isNotBlank(candidate) && supports.contains(candidate) && !ordered.contains(candidate)) {
                ordered.add(candidate);
            }
        }
        return ordered.isEmpty() ? null : ordered.get(0);
    }

    private static void addByMarkerFile(Project project, String fileName, Set<String> candidates, String... languages) {
        if (project == null || StringUtils.isBlank(fileName)) {
            return;
        }
        if (hasProjectFile(project, fileName)) {
            addCandidates(candidates, languages);
        }
    }

    private static boolean hasProjectFile(Project project, String fileName) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        if (fileName.startsWith(".") && fileName.indexOf('.', 1) < 0) {
            return hasFileBySuffix(project, fileName, scope);
        }
        return !FilenameIndex.getVirtualFilesByName(project, fileName, scope).isEmpty()
                || hasFileBySuffix(project, fileName, scope);
    }

    private static boolean hasFileBySuffix(Project project, String suffix, GlobalSearchScope scope) {
        if (!suffix.startsWith(".")) {
            return false;
        }
        String extension = suffix.substring(1);
        if (StringUtils.isBlank(extension)) {
            return false;
        }
        for (VirtualFile file : FilenameIndex.getAllFilesByExt(project, extension, scope)) {
            if (file.getName().endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static void addCandidates(Set<String> candidates, String... languages) {
        if (languages == null) {
            return;
        }
        for (String language : languages) {
            if (StringUtils.isNotBlank(language)) {
                candidates.add(language);
            }
        }
    }

    private static String firstSupported(List<String> supports, String... candidates) {
        if (supports == null || supports.isEmpty() || candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (StringUtils.isNotBlank(candidate) && supports.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String normalizeRequestLanguage(String language, List<String> supports) {
        if ("TypeScript".equals(language) && supports.contains("JavaScript")) {
            return "JavaScript";
        }
        return language;
    }
}
