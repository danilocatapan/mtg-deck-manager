package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class BracketMetaPolicy {

    public String normalizeBracket(String bracket) {
        if (bracket == null || bracket.isBlank()) return "casual";
        String normalized = bracket.trim().toLowerCase(Locale.ROOT).replace("_", "-");
        if (normalized.equals("highpower")) return "high-power";
        return switch (normalized) {
            case "casual", "mid", "high-power", "cedh" -> normalized;
            default -> "casual";
        };
    }

    public String normalizeSourceMode(String sourceMode) {
        if (sourceMode == null || sourceMode.isBlank()) return "auto";
        String normalized = sourceMode.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "auto", "casual_meta", "competitive_meta", "cedh_only", "local_only" -> normalized;
            default -> "auto";
        };
    }

    public List<String> sourcesFor(String bracket, String sourceMode) {
        String normalizedMode = normalizeSourceMode(sourceMode);
        if ("local_only".equals(normalizedMode)) return List.of("LOCAL");
        if ("casual_meta".equals(normalizedMode)) return List.of("EDHREC", "LOCAL", "SCRYFALL_CACHE");
        if ("competitive_meta".equals(normalizedMode)) return List.of("TOPDECK", "SPICERACK", "EDHTOP16", "LOCAL");
        if ("cedh_only".equals(normalizedMode)) return List.of("EDHTOP16", "TOPDECK", "SPICERACK");

        return switch (normalizeBracket(bracket)) {
            case "cedh" -> List.of("EDHTOP16", "TOPDECK", "SPICERACK", "LOCAL");
            case "high-power" -> List.of("TOPDECK", "SPICERACK", "EDHTOP16", "EDHREC", "LOCAL");
            case "mid" -> List.of("EDHREC", "TOPDECK", "SPICERACK", "LOCAL");
            default -> List.of("EDHREC", "LOCAL", "SCRYFALL_CACHE");
        };
    }

    public List<MetaSourceStatus> sourceStatuses() {
        OffsetDateTime now = OffsetDateTime.now();
        return List.of(
                new MetaSourceStatus("TopDeck.gg", true, now, List.of("high-power", "cedh"), "competitive_meta"),
                new MetaSourceStatus("Spicerack", true, now, List.of("mid", "high-power", "cedh"), "competitive_meta"),
                new MetaSourceStatus("EDHTop16", true, now, List.of("cedh"), "cedh_only"),
                new MetaSourceStatus("EDHREC", true, now, List.of("casual", "mid"), "casual_meta"),
                new MetaSourceStatus("LOCAL", true, now, List.of("casual", "mid", "high-power", "cedh"), "fallback")
        );
    }
}
