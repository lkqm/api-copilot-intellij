package io.apix.window.support;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Request preview state.
 */
@Getter
@AllArgsConstructor
public enum PreviewState {
    HIDDEN,
    VERTICAL,
    ;

    public PreviewState switchState() {
        int idx = (this.ordinal() + 1) % PreviewState.values().length;
        return PreviewState.values()[idx];
    }

}
