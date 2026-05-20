package com.mtg.dto;

import java.util.List;
import java.util.Map;

public record SecurityStatusResponseDTO(
        String status,
        String environment,
        String generatedAt,
        List<SecurityIssueDTO> issues,
        List<String> recommendations,
        Map<String, Object> details
) {
}
