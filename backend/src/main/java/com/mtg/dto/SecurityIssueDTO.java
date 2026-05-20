package com.mtg.dto;

public record SecurityIssueDTO(
        String type,
        String severity,
        String description,
        String recommendation
) {
}
