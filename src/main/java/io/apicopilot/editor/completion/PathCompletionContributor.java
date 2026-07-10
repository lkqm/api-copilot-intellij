package io.apicopilot.editor.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ProcessingContext;
import io.apicopilot.document.Document;
import io.apicopilot.document.DocumentManager;
import io.apicopilot.util.PathUtils;
import io.swagger.v3.oas.models.OpenAPI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;


/**
 * API路径代码完成提示.
 */
public class PathCompletionContributor extends CompletionContributor {

    public PathCompletionContributor() {
        CompletionProvider<CompletionParameters> provider = new DefaultCompletionProvider();
        this.extend(CompletionType.BASIC, psiElement(), provider);
    }


    private static class DefaultCompletionProvider extends CompletionProvider<CompletionParameters> {


        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
            Editor editor = parameters.getEditor();
            com.intellij.openapi.editor.Document editorDocument = editor.getDocument();
            Project project = editor.getProject();
            if (project == null) {
                return;
            }

            // 获取匹配的path前缀
            String prefix = result.getPrefixMatcher().getPrefix();
            int offset = editor.getCaretModel().getOffset();
            if (!prefix.startsWith("/")) {
                String preText = editorDocument.getText(TextRange.create(editor.getCaretModel().getVisualLineStart(), offset - prefix.length()));
                String prePath = PathUtils.getMatchPathAtEnd(preText);
                if (prePath == null) {
                    return;
                }
                prefix = prePath + prefix;
            }

            DocumentManager documentManager = DocumentManager.getInstance(project);
            List<Document> documents = documentManager.getDocuments();
            List<LookupElementBuilder> elements = new ArrayList<>();
            documents.forEach(document -> {
                if (document == null) {
                    return;
                }
                OpenAPI openApi = document.getOpenApi();
                if (openApi == null || openApi.getPaths() == null) {
                    return;
                }
                openApi.getPaths().forEach((path, pathItem) -> {
                    if (pathItem == null) {
                        return;
                    }
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        elements.add(LookupElementBuilder
                                .create(path)
                                .withTailText(" [" + method.name() + "]" + operation.getSummary())

                        );
                    });
                });
            });
            result = result.withPrefixMatcher(new MyPrefixMatcher(prefix));
            result.addAllElements(elements);
        }
    }

    static class MyPrefixMatcher extends PrefixMatcher {


        public MyPrefixMatcher(String prefix) {
            super(prefix);
        }

        @Override
        public boolean prefixMatches(@NotNull String name) {
            return name.startsWith(getPrefix());
        }

        @Override
        public int matchingDegree(String string) {
            return string.startsWith(getPrefix()) ? 1 : 0;
        }

        @Override
        public @NotNull
        PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
            return new MyPrefixMatcher(prefix);
        }

    }

}
