package io.apix.util;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class PathUtils {

    private static final Pattern PATH_PATTERN = Pattern.compile(".*?(/[a-zA-Z0-9\\-_/{}]*)$");

    /**
     * 获取文本中末尾匹配的Path
     *
     * @param target 待匹配的文本内容
     * @return 返回匹配的Path，返回null未匹配
     */
    public static String getMatchPathAtEnd(String target) {
        Matcher matcher = PATH_PATTERN.matcher(target);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * 判断路径是否匹配
     *
     * @param path       文档中的路径
     * @param targetPath 代码中的路径
     * @return 是否匹配
     */
    public static MatchResult isMatch(@NotNull String path, @NotNull String targetPath) {
        if (path.equals(targetPath)) {
            return MatchResult.YES;
        }
        String[] segments1 = path.split("/");
        String[] segments2 = targetPath.split("/");
        if (segments1.length != segments2.length) {
            return MatchResult.NO;
        }

        for (int i = 0; i < segments1.length; i++) {
            String segment1 = segments1[i];
            String segment2 = segments2[i];
            if (!segment1.equals(segment2) && !segment1.startsWith("{")) {
                return MatchResult.NO;
            }
        }
        return new MatchResult(true, false);
    }

    @Getter
    @Builder
    public static class MatchResult {

        public static final MatchResult YES = new MatchResult(true);
        public static final MatchResult NO = new MatchResult(false);

        /**
         * 是否匹配
         */
        private final boolean matched;

        /**
         * 是否精准匹配
         */
        private final boolean exact;

        public MatchResult(boolean matched) {
            this.matched = matched;
            this.exact = matched;
        }

        public MatchResult(boolean matched, boolean exact) {
            this.matched = matched;
            this.exact = exact;
        }
    }

}
