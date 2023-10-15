package io.apicopilot.util;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Psi字符串常量相关工具.
 */
@UtilityClass
public class PsiStringLiteralUtils {


    /**
     * 计算字符串类型
     */
    @NotNull
    public StringLiteralType getStringLiteralType(String text) {
        String[] splitters = {"\"\"\"", "\"", "'", "`"};
        for (String splitter : splitters) {
            if (text.length() > splitter.length() && text.startsWith(splitter)) {
                boolean isSame = text.endsWith(splitter);
                return new StringLiteralType(true, splitter, isSame ? splitter : null);
            }
        }
        return StringLiteralType.NONE;
    }

    /**
     * 计算字符常量的值
     */
    public static String getStringLiteralValue(String text) {
        StringLiteralType type = getStringLiteralType(text);
        return type == StringLiteralType.NONE ? text : text.substring(type.getStartLength(), text.length() - type.getEndLength());
    }

    /**
     * 是否是字符串
     */
    public static boolean isInStringLiteral(PsiElement element) {
        ParserDefinition definition = LanguageParserDefinitions.INSTANCE.forLanguage(PsiUtilCore.findLanguageFromElement(element));
        return definition != null && (isStringLiteral(element, definition) || isStringLiteral(element.getParent(), definition));
    }

    private static boolean isStringLiteral(PsiElement element, ParserDefinition definition) {
        return ((PsiElementPattern.Capture<?>) PlatformPatterns.psiElement().withElementType(definition.getStringLiteralElements())).accepts(element);
    }

    @AllArgsConstructor
    @Getter
    public static class StringLiteralType {
        public static StringLiteralType NONE = new StringLiteralType(true, null, null);
        private final boolean shouldSame;
        private final String start;
        private final String end;

        public int getStartLength() {
            return start != null ? start.length() : 0;
        }

        public int getEndLength() {
            return end != null ? end.length() : 0;
        }
    }

}
