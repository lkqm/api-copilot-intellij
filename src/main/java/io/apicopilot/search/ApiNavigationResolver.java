package io.apicopilot.search;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ApiNavigationResolver {

    @NotNull
    List<ApiNavigationData> getApis();
}
