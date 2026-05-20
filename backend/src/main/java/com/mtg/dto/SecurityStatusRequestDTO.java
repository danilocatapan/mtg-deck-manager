package com.mtg.dto;

public record SecurityStatusRequestDTO(
        Boolean includeDetails,
        Boolean scanExternalDependencies
) {
    public boolean includeDetailsEnabled() {
        return Boolean.TRUE.equals(includeDetails);
    }

    public boolean scanExternalDependenciesEnabled() {
        return Boolean.TRUE.equals(scanExternalDependencies);
    }
}
