package com.mtg.dto;

import java.util.List;

public record CardCollectionRequestDTO(
        List<String> names
) {
}
