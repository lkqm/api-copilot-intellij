package apicopilot.util;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class OpenApiUtils {


    /**
     * 获取接口数量
     */
    public static int countApi(OpenAPI openApi) {
        AtomicInteger count = new AtomicInteger();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                count.getAndIncrement();
            });
        });
        return count.get();
    }

}
