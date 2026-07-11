package io.apix.codegen.context;

import lombok.*;

import java.util.HashMap;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LastestContext extends HashMap<String, Object> {

    private List<File> files;


    @Data
    @Builder
    public static class File {
        private String type;
        private String path;
    }
}
