package io.apix.editor.completion;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import org.jetbrains.annotations.Nullable;

/**
 * 解决完成提示过程中某些符号会中断代码提示.
 */
public class PathCompletionCharFilter extends CharFilter {
    @Override
    public @Nullable Result acceptChar(char c, int i, Lookup lookup) {
        if (lookup.isCompletion() && (c == '/' || c == '{' || c == '}')) {
            return Result.ADD_TO_PREFIX;
        }
        return null;
    }
}
