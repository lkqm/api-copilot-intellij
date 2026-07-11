package io.apix.document.resolver;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.apix.document.Document;
import io.apix.util.HttpUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Resolve API document from OpenAPI.
 */
@AllArgsConstructor
public class OpenAPIApiResolver extends AbstractApiResolver {

    private final Project project;
    private final Document document;

    @SneakyThrows
    @Override
    protected ResolveResult getContent(boolean reload) {
        if (StringUtils.isNotEmpty(document.getContent()) && !reload) {
            return ResolveResult.ok(document.getContent());
        }

        Document.OpenApiConfig openApiConfig = document.getOpenApiConfig();
        String path = openApiConfig.getPath();
        if (StringUtils.isEmpty(path)) {
            return ResolveResult.fail("invalid document config");
        }

        if (openApiConfig.isRemote()) {
            try {
                String content = HttpUtils.downloadText(path, Duration.ofSeconds(10));
                return ResolveResult.ok(content);
            } catch (IOException e) {
                return ResolveResult.fail("download file failed: " + ExceptionUtils.getMessage(e));
            }
        } else {
            VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
            if (projectDir != null) {
                VirtualFile file = projectDir.findFileByRelativePath(path);
                if (file != null) {
                    String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                    return ResolveResult.ok(content);
                }
            }
            return ResolveResult.fail("file not exists: " + path);
        }
    }

}
