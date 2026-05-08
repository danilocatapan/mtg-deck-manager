package com.mtg.dto;

public record AppInfoResponseDTO(
        String name,
        String version,
        String commit,
        String branch,
        String buildTime,
        String environment,
        String creator,
        String objective
) {
}
