package com.mtg.service;

import com.mtg.domain.DeckAnalysis;
import com.mtg.dto.ComparisonMetricDTO;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.dto.SimilarDeckComparisonDTO;
import com.mtg.model.Deck;
import com.mtg.repository.DeckRepository;
import com.mtg.service.meta.BracketMetaPolicy;
import com.mtg.service.meta.CommanderMetaProfile;
import com.mtg.service.meta.CommanderMetaProfileService;
import com.mtg.service.meta.RoleTargets;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DeckComparisonService {

    @Inject
    DeckRepository deckRepository;

    @Inject
    DeckAnalysisService deckAnalysisService;

    @Inject
    CommanderMetaProfileService commanderMetaProfileService;

    @Inject
    BracketMetaPolicy bracketMetaPolicy;

    public SimilarDeckComparisonDTO compare(Long deckId, RecommendationParamsDTO params, String ownerId) {
        Deck deck = deckRepository.findByIdAndOwner(deckId, ownerId);
        if (deck == null) {
            throw new NotFoundException("Deck not found");
        }
        String bracket = (bracketMetaPolicy == null ? new BracketMetaPolicy() : bracketMetaPolicy)
                .normalizeBracket(params == null ? null : params.bracket());
        DeckAnalysis analysis = deckAnalysisService.analyzeDeck(deckId, ownerId);
        CommanderMetaProfile profile = commanderMetaProfileService == null
                ? null
                : commanderMetaProfileService.findByCommanderAndBracket(deck.getCommander(), bracket);
        RoleTargets targets = RoleTargets.forBracket(bracket);
        int sampleSize = profile == null ? 0 : profile.sampleSize();
        List<String> sources = profile == null ? List.of("LOCAL") : profile.sourcesUsed();

        List<ComparisonMetricDTO> metrics = new ArrayList<>();
        metrics.add(metric("lands", "Terrenos", analysis.manaBase().landCount(), landTarget(bracket), 2));
        metrics.add(metric("ramp", "Ramp", analysis.rampCount(), targets.ramp(), 1));
        metrics.add(metric("draw", "Compra", analysis.drawCount(), targets.draw(), 1));
        metrics.add(metric("interaction", "Interacao", analysis.interactionCount(), targets.removal(), 1));
        metrics.add(metric("curve", "CMC medio", analysis.averageCmc(), curveTarget(bracket), 0.25, true));
        metrics.add(metric("gameChangers", "Game Changers", estimateGameChangers(analysis), gameChangerTarget(bracket), 1));
        metrics.add(metric("combos", "Combos", analysis.combos().present().size(), comboTarget(bracket), 1));

        return new SimilarDeckComparisonDTO(deck.getCommander(), bracket, sampleSize, sources, metrics);
    }

    private ComparisonMetricDTO metric(String key, String label, double current, double target, double tolerance) {
        return metric(key, label, current, target, tolerance, false);
    }

    private ComparisonMetricDTO metric(String key, String label, double current, double target, double tolerance, boolean lowerIsBetter) {
        double delta = lowerIsBetter ? target - current : current - target;
        String status = Math.abs(current - target) <= tolerance ? "ok" : delta >= 0 ? "above" : "below";
        String message = switch (status) {
            case "below" -> label + " abaixo da media-alvo para decks similares.";
            case "above" -> lowerIsBetter ? label + " acima da curva media; considere baixar custos." : label + " acima da media-alvo.";
            default -> label + " proximo da media-alvo.";
        };
        return new ComparisonMetricDTO(key, label, round(current), round(target), status, message);
    }

    private int landTarget(String bracket) {
        return switch (bracket == null ? "casual" : bracket) {
            case "cedh" -> 29;
            case "high-power" -> 32;
            case "mid" -> 35;
            default -> 37;
        };
    }

    private double curveTarget(String bracket) {
        return switch (bracket == null ? "casual" : bracket) {
            case "cedh" -> 2.1;
            case "high-power" -> 2.7;
            case "mid" -> 3.1;
            default -> 3.4;
        };
    }

    private int gameChangerTarget(String bracket) {
        return switch (bracket == null ? "casual" : bracket) {
            case "cedh", "high-power" -> 4;
            case "mid" -> 2;
            default -> 1;
        };
    }

    private int comboTarget(String bracket) {
        return switch (bracket == null ? "casual" : bracket) {
            case "cedh" -> 2;
            case "high-power" -> 1;
            default -> 0;
        };
    }

    private int estimateGameChangers(DeckAnalysis analysis) {
        return Math.max(0, analysis.combos().present().size()
                + analysis.combos().oneCardAway().size() / 2
                + Math.max(0, analysis.score().threat() - 70) / 15);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
