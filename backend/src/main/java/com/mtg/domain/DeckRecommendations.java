package com.mtg.domain;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(name = "DeckRecommendations")
public record DeckRecommendations(
        List<RecommendationItem> add,
        List<RecommendationItem> cut,
        Map<String, Integer> gaps
) {
}
