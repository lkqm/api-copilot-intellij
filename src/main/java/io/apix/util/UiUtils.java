package io.apix.util;

import com.intellij.openapi.application.ApplicationManager;

/**
 * 异步执行工具
 */
public class UiUtils {


    public static void run(Runnable runnable) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }

}
