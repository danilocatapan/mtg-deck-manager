package com.mtg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.meta.MetaCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RecommendationBenchmarkScenarioService {
    @Inject StrategicRecommendationEngine engine;
    @Inject ObjectMapper objectMapper;

    public Execution execute(JsonNode fixture) {
        Deck deck = deck(fixture);
        Map<String, CardResponseDTO> knownCards = knownCards(fixture);
        List<MetaCard> metaCards = metaCards(fixture);
        Set<String> colors = textSet(fixture.path("colorIdentity"));
        StrategicRecommendationEngine.Result result = engine.recommend(new StrategicRecommendationEngine.Scenario(
                deck,
                metaCards,
                knownCards,
                colors,
                fixture.path("bracket").asText("mid"),
                !metaCards.isEmpty(),
                fixture.path("strategy").asText(null),
                fixture.path("budget").isNumber() ? fixture.path("budget").asDouble() : null,
                textSet(fixture.path("filters")),
                textSet(fixture.path("collection")),
                fixture.path("maxRecommendations").asInt(10),
                fixture.path("metaSampleSize").asInt(metaCards.size()),
                List.of("benchmark_frozen"),
                fixture.path("currentGameChangers").asInt(0)
        ));
        ArrayNode output = objectMapper.createArrayNode();
        result.recommendations().forEach(item -> output.add(recommendation(item)));
        return new Execution(output, result);
    }

    public Validation validate(JsonNode fixture, JsonNode output) {
        Set<String> deck = textSet(fixture.path("deck"));
        Set<String> protectedCards = textSet(fixture.path("labels").path("protectedCards"));
        protectedCards.add(normalize(fixture.path("commander").asText()));
        Set<String> seenAdds = new LinkedHashSet<>();
        List<String> violations = new ArrayList<>();
        for (JsonNode item : output) {
            String add = normalize(item.path("add").asText());
            String remove = normalize(item.path("remove").asText());
            if (add.isBlank() || remove.isBlank()) violations.add("missing_add_or_cut");
            if (!remove.isBlank() && !deck.contains(remove)) violations.add("cut_not_in_deck:" + remove);
            if (protectedCards.contains(remove)) violations.add("protected_cut:" + remove);
            if (deck.contains(add) || !seenAdds.add(add)) violations.add("illegal_duplicate:" + add);
            if (item.path("offColor").asBoolean(false)) violations.add("off_color:" + add);
            if (item.path("overBudget").asBoolean(false)) violations.add("budget_violation:" + add);
            if (item.path("outsideCollection").asBoolean(false)) violations.add("collection_violation:" + add);
            if (item.path("restrictionViolation").asBoolean(false)) violations.add("restriction_violation:" + add);
        }
        return new Validation(List.copyOf(new LinkedHashSet<>(violations)));
    }

    public List<String> validateFixture(JsonNode fixture) {
        List<String> violations = new ArrayList<>();
        String source = fixture.path("source").asText();
        String sourceUrl = fixture.path("sourceUrl").asText();
        String capturedAt = fixture.path("capturedAt").asText();
        int deckSize = fixture.path("deck").isArray()
                ? java.util.stream.StreamSupport.stream(fixture.path("deck").spliterator(), false)
                        .mapToInt(item -> item.isTextual() ? 1 : item.path("quantity").asInt(1))
                        .sum()
                : 0;
        if (!Set.of("archidekt_popular", "topdeck_tournament").contains(source)) violations.add("unapproved_source");
        if (!sourceUrl.startsWith("https://")) violations.add("missing_source_url");
        if (capturedAt.isBlank()) violations.add("missing_capture_date");
        if (fixture.path("commander").asText().isBlank()) violations.add("missing_commander");
        if (deckSize != 100) violations.add("deck_must_include_commander_plus_99");
        return List.copyOf(violations);
    }

    private Deck deck(JsonNode fixture) {
        List<DeckCard> cards = new ArrayList<>();
        String commander = fixture.path("commander").asText();
        fixture.path("deck").forEach(item -> {
            String name = item.isTextual() ? item.asText() : item.path("name").asText();
            int quantity = item.isTextual() ? 1 : item.path("quantity").asInt(1);
            if (!name.isBlank() && !name.equalsIgnoreCase(commander)) cards.add(new DeckCard(name, quantity));
        });
        Deck deck = new Deck(fixture.path("id").asText(), commander, cards);
        deck.setColorIdentity(String.join(",", textSet(fixture.path("colorIdentity"))));
        return deck;
    }

    private Map<String, CardResponseDTO> knownCards(JsonNode fixture) {
        Map<String, CardResponseDTO> cards = new HashMap<>();
        addCards(cards, fixture.path("deck"), fixture);
        addCards(cards, fixture.path("catalog"), fixture);
        addCards(cards, fixture.path("meta"), fixture);
        String commander = fixture.path("commander").asText();
        cards.putIfAbsent(normalize(commander), card(commander, fixture));
        return cards;
    }

    private void addCards(Map<String, CardResponseDTO> cards, JsonNode source, JsonNode fixture) {
        source.forEach(item -> {
            String name = item.isTextual() ? item.asText() : item.path("name").asText();
            if (!name.isBlank()) cards.putIfAbsent(normalize(name), card(name, item.isObject() ? item : fixture));
        });
    }

    private CardResponseDTO card(String name, JsonNode source) {
        List<String> colors = source.path("colorIdentity").isArray()
                ? textList(source.path("colorIdentity"))
                : List.of();
        String typeLine = source.path("typeLine").asText(inferType(name));
        String oracle = source.path("oracleText").asText(inferOracle(typeLine));
        double cmc = source.path("cmc").asDouble(typeLine.contains("Land") ? 0.0 : 3.0);
        return new CardResponseDTO(name, "", typeLine, oracle, cmc, colors, List.of(), null, source.path("price").isNumber() ? source.path("price").asDouble() : null);
    }

    private List<MetaCard> metaCards(JsonNode fixture) {
        List<MetaCard> result = new ArrayList<>();
        fixture.path("meta").forEach(item -> {
            String name = item.isTextual() ? item.asText() : item.path("name").asText();
            if (!name.isBlank()) result.add(new MetaCard(name, item.path("inclusion").asDouble(0.65), item.path("role").asText("synergy"), item.path("cmc").isNumber() ? item.path("cmc").asDouble() : null));
        });
        return result;
    }

    private JsonNode recommendation(StrategicRecommendation item) {
        return objectMapper.valueToTree(Map.of(
                "add", safe(item.add()),
                "remove", safe(item.remove()),
                "reasoning", safe(item.reasoning()),
                "risk", safe(item.risk())
        ));
    }

    private Set<String> textSet(JsonNode node) {
        Set<String> result = new LinkedHashSet<>();
        if (!node.isArray()) return result;
        node.forEach(item -> result.add(normalize(item.isTextual() ? item.asText() : item.path("name").asText())));
        return result;
    }

    private List<String> textList(JsonNode node) {
        List<String> result = new ArrayList<>();
        node.forEach(item -> result.add(item.asText()));
        return result;
    }

    private String inferType(String name) {
        return name.toLowerCase(Locale.ROOT).contains("land") ? "Basic Land" : "Creature";
    }

    private String inferOracle(String typeLine) {
        return typeLine.contains("Land") ? "Add one mana." : "A benchmark fixture card.";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record Execution(JsonNode output, StrategicRecommendationEngine.Result result) {}
    public record Validation(List<String> violations) {
        public boolean passed() { return violations.isEmpty(); }
    }
}
