package apicopilot.apidoc;

import apicopilot.model.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

@AllArgsConstructor
public class FileDocumentProvider implements DocumentProvider {

    private final Project project;

    @Override
    public Document getDocument() {
        VirtualFile file = findFile();
        if (file == null) {
            return null;
        }
        Document document = new Document();
        document.setOpenApi(resolveFile(file));
        return document;
    }

    @SneakyThrows
    private OpenAPI resolveFile(VirtualFile vf) {
        String openapiContent = new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
        SwaggerParseResult parseResult = new OpenAPIParser().readContents(openapiContent, null, null);
        return parseResult.getOpenAPI();
    }

    private VirtualFile findFile() {
        VirtualFile openapiFile = null;

        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
            String[] filenames = {"openapi.yaml", "openapi.yml", "openapi.json"};
            for (String filename : filenames) {
                openapiFile = projectDir.findFileByRelativePath(filename);
                if (openapiFile != null) {
                    break;
                }
            }
        }
        return openapiFile;
    }
}
