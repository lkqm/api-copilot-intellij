package io.apicopilot.codegen.context;

import lombok.*;

import java.util.HashMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OptionContext extends HashMap<String, Object> {

    private String doc;
    private String tag;

}
