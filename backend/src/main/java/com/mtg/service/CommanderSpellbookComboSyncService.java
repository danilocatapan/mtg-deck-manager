package com.mtg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mtg.client.CommanderSpellbookClient;
import com.mtg.model.MetaCombo;
import com.mtg.model.MetaComboCard;
import com.mtg.repository.MetaComboRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class CommanderSpellbookComboSyncService {
    private static final Logger LOG = Logger.getLogger(CommanderSpellbookComboSyncService.class);
    private static final String SOURCE = "Commander Spellbook";

    @Inject
    @RestClient
    CommanderSpellbookClient client;

    @Inject
    MetaComboRepository repository;

    @Inject
    ComboDetectionService comboDetectionService;

    @Transactional
    public int syncCommanderCombos() {
        return sync("legal:commander", 500);
    }

    @Transactional
    public int sync(String query, int limit) {
        JsonNode payload = client.searchVariants(query == null || query.isBlank() ? "legal:commander" : query, Math.max(1, limit));
        List<JsonNode> variants = variantsFrom(payload).stream().limit(Math.max(1, limit)).toList();
        int imported = 0;
        for (JsonNode variant : variants) {
            MetaCombo combo = toCombo(variant);
            if (combo == null || combo.getCards().isEmpty()) {
                continue;
            }
            MetaCombo current = repository.findBySourceAndExternalId(combo.getSource(), combo.getExternalId());
            if (current == null) {
                repository.persist(combo);
            } else {
                copyInto(current, combo);
            }
            imported++;
        }
        LOG.infov("event=combo.sync.completed source={0} imported={1}", SOURCE, imported);
        if (comboDetectionService != null) {
            comboDetectionService.clearCache();
        }
        return imported;
    }

    private List<JsonNode> variantsFrom(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return List.of();
        }
        if (payload.isArray()) {
            return stream(payload).toList();
        }
        JsonNode results = payload.get("results");
        if (results != null && results.isArray()) {
            return stream(results).toList();
        }
        JsonNode variants = payload.get("variants");
        if (variants != null && variants.isArray()) {
            return stream(variants).toList();
        }
        return List.of(payload);
    }

    private MetaCombo toCombo(JsonNode variant) {
        String externalId = firstText(variant, "spellbookId", "spellbook_id", "id", "pk");
        String name = firstText(variant, "name", "variantName", "variant_name");
        List<CardPart> cards = cardsFrom(variant);
        if ((name == null || name.isBlank()) && !cards.isEmpty()) {
            name = cards.stream().map(CardPart::name).collect(Collectors.joining(" + "));
        }
        if (externalId == null || externalId.isBlank()) {
            externalId = normalize(name);
        }
        if (name == null || name.isBlank() || externalId == null || externalId.isBlank()) {
            return null;
        }

        MetaCombo combo = new MetaCombo();
        combo.setSource(SOURCE);
        combo.setExternalId(externalId);
        combo.setName(name);
        combo.setResultText(resultsFrom(variant));
        combo.setTags(joinTextArray(firstNode(variant, "tags", "features")));
        combo.setLegalities(joinTextArray(firstNode(variant, "legalities", "formats")));
        combo.setBrackets(joinTextArray(firstNode(variant, "brackets", "bracket")));
        combo.setCommanderRequired(firstText(variant, "commander", "commanderRequired", "commander_required"));
        combo.setPopularity(firstInt(variant, "popularity", "edhrecCount", "edhrec_count", "deck_count"));
        combo.setSourceUrl(firstText(variant, "url", "link", "publicUrl", "public_url"));
        combo.setSourceUpdatedAt(firstDate(variant, "updated", "updatedAt", "updated_at"));
        combo.setSyncedAt(OffsetDateTime.now());
        combo.setCards(cards.stream().map(this::toCard).toList());
        return combo;
    }

    private MetaComboCard toCard(CardPart part) {
        MetaComboCard card = new MetaComboCard();
        card.setCardName(part.name());
        card.setCardNormalized(normalize(part.name()));
        card.setCardRole(part.role());
        card.setCommanderSlot(part.commanderSlot());
        return card;
    }

    private void copyInto(MetaCombo current, MetaCombo next) {
        current.setName(next.getName());
        current.setResultText(next.getResultText());
        current.setTags(next.getTags());
        current.setLegalities(next.getLegalities());
        current.setBrackets(next.getBrackets());
        current.setCommanderRequired(next.getCommanderRequired());
        current.setPopularity(next.getPopularity());
        current.setSourceUrl(next.getSourceUrl());
        current.setSourceUpdatedAt(next.getSourceUpdatedAt());
        current.setSyncedAt(next.getSyncedAt());
        current.setCards(next.getCards());
    }

    private List<CardPart> cardsFrom(JsonNode variant) {
        List<CardPart> cards = new ArrayList<>();
        collectCards(cards, firstNode(variant, "cards", "uses"), false);
        collectCards(cards, firstNode(variant, "commanders", "requiresCommander"), true);
        String commander = firstText(variant, "commander", "commanderRequired", "commander_required");
        if (commander != null && !commander.isBlank()) {
            cards.add(new CardPart(commander, "commander", true));
        }
        return cards.stream()
                .filter(card -> card.name() != null && !card.name().isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(card -> normalize(card.name()), card -> card, (left, right) -> left, java.util.LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private void collectCards(List<CardPart> cards, JsonNode node, boolean commanderSlot) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectCards(cards, item, commanderSlot);
            }
            return;
        }
        if (node.isObject()) {
            String name = firstText(node, "name", "card", "cardName", "card_name");
            String role = firstText(node, "role", "template");
            if (name != null && !name.isBlank()) {
                cards.add(new CardPart(name, role, commanderSlot));
            }
            return;
        }
        if (node.isTextual()) {
            cards.add(new CardPart(node.asText(), null, commanderSlot));
        }
    }

    private String resultsFrom(JsonNode variant) {
        String direct = firstText(variant, "result", "resultText", "result_text", "description");
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        return joinTextArray(firstNode(variant, "results", "produces"));
    }

    private JsonNode firstNode(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            JsonNode child = node.get(name);
            if (child != null && !child.isNull()) {
                return child;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... names) {
        JsonNode child = firstNode(node, names);
        if (child == null || child.isNull()) {
            return null;
        }
        if (child.isTextual() || child.isNumber() || child.isBoolean()) {
            return child.asText();
        }
        return firstText(child, "name", "label", "value");
    }

    private Integer firstInt(JsonNode node, String... names) {
        JsonNode child = firstNode(node, names);
        if (child == null || child.isNull()) {
            return null;
        }
        if (child.isInt() || child.isLong()) {
            return child.asInt();
        }
        if (child.isTextual()) {
            try {
                return Integer.parseInt(child.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private OffsetDateTime firstDate(JsonNode node, String... names) {
        String value = firstText(node, names);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String joinTextArray(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return stream(node)
                    .map(this::nodeLabel)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .collect(Collectors.joining(","));
        }
        return nodeLabel(node);
    }

    private String nodeLabel(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        return firstText(node, "name", "label", "value");
    }

    private Iterable<JsonNode> iterable(JsonNode node) {
        return node::elements;
    }

    private java.util.stream.Stream<JsonNode> stream(JsonNode node) {
        return StreamSupport.stream(iterable(node).spliterator(), false);
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private record CardPart(String name, String role, boolean commanderSlot) {
    }
}
