package com.mtg.service.synergy;

import com.mtg.dto.CardResponseDTO;
import com.mtg.domain.CommanderProfile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class SynergyEngine {

    @Inject
    CardTagger tagger;

    public Set<String> tagsForCard(CardResponseDTO card) {
        return tagger.tagCard(card);
    }

    public Set<String> tagsForCommanderProfile(CommanderProfile profile) {
        if (profile == null) return new HashSet<>();
        if (profile.tags() != null && !profile.tags().isEmpty()) return new HashSet<>(profile.tags());
        // fallback: derive basic tags from colors
        Set<String> tags = new HashSet<>();
        if (profile.colors() != null) {
            for (String c : profile.colors()) tags.add("color_" + c.toLowerCase());
        }
        return tags;
    }

    public Set<String> aggregateTags(List<CardResponseDTO> cards) {
        Set<String> tags = new HashSet<>();
        if (cards == null) return tags;
        for (CardResponseDTO c : cards) {
            tags.addAll(tagsForCard(c));
        }
        return tags;
    }

    public double computeSynergy(Set<String> cardTags, Set<String> deckTags, Set<String> commanderTags) {
        if (cardTags == null || cardTags.isEmpty()) return 0.0;
        Set<String> pool = new HashSet<>();
        if (deckTags != null) pool.addAll(deckTags);
        if (commanderTags != null) pool.addAll(commanderTags);
        if (pool.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(cardTags);
        intersection.retainAll(pool);
        double synergy = (double) intersection.size() / (double) pool.size();
        return Math.max(0.0, Math.min(1.0, synergy));
    }
}
