package com.mtg.service;

import com.mtg.dto.AuthenticatedUserDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mtg.dto.CommanderDTO;
import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckHistoryEntryDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.dto.RecommendationAuditExportDTO;
import com.mtg.dto.UserCollectionCardDTO;
import com.mtg.dto.UserDataExportDTO;
import com.mtg.model.Deck;
import com.mtg.model.RecommendationAuditRun;
import com.mtg.repository.DeckLikeRepository;
import com.mtg.repository.DeckRepository;
import com.mtg.repository.RecommendationAuditRepository;
import com.mtg.repository.UserCardCollectionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserPrivacyService {
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final List<String> COLLECTED_DATA = List.of(
            "Google subject/id",
            "nome",
            "e-mail",
            "avatar",
            "decks cadastrados",
            "cartas dos decks",
            "colecao de cartas importada pelo usuario",
            "historico e auditorias de recomendacao"
    );

    private final DeckRepository deckRepository;
    private final RecommendationAuditRepository auditRepository;
    private final DeckLikeRepository deckLikeRepository;
    private final UserCardCollectionRepository collectionRepository;

    @Inject
    public UserPrivacyService(
            DeckRepository deckRepository,
            RecommendationAuditRepository auditRepository,
            DeckLikeRepository deckLikeRepository,
            UserCardCollectionRepository collectionRepository
    ) {
        this.deckRepository = deckRepository;
        this.auditRepository = auditRepository;
        this.deckLikeRepository = deckLikeRepository;
        this.collectionRepository = collectionRepository;
    }

    public UserDataExportDTO exportData(AuthenticatedUserDTO user) {
        validateUser(user);
        String ownerId = user.googleSubject();
        return new UserDataExportDTO(
                OffsetDateTime.now(ZoneOffset.UTC),
                user,
                COLLECTED_DATA,
                deckRepository.listByOwner(ownerId).stream()
                        .map(this::toDeckDto)
                        .collect(Collectors.toList()),
                collectionRepository.listByOwner(ownerId).stream()
                        .map(item -> new UserCollectionCardDTO(item.getCardName(), item.getQuantity() == null ? 0 : item.getQuantity()))
                        .collect(Collectors.toList()),
                auditRepository.listByOwner(ownerId).stream()
                        .map(this::toAuditDto)
                        .collect(Collectors.toList())
        );
    }

    @Transactional
    public void deleteAccountData(String ownerId) {
        validateOwner(ownerId);
        auditRepository.delete("ownerId", ownerId);
        deckLikeRepository.deleteByOwner(ownerId);
        collectionRepository.deleteByOwner(ownerId);
        deckRepository.listByOwner(ownerId).forEach(deckRepository::delete);
    }

    private DeckResponseDTO toDeckDto(Deck deck) {
        List<DeckCardDTO> cards = deck.getCards().stream()
                .map(card -> new DeckCardDTO(card.getName(), card.getQuantity()))
                .collect(Collectors.toList());
        return new DeckResponseDTO(
                deck.getId(),
                deck.getName(),
                deck.getCommander(),
                cards,
                deck.getColorIdentity(),
                commandersFor(deck),
                historyFor(deck),
                deck.getVisibility()
        );
    }

    private List<CommanderDTO> commandersFor(Deck deck) {
        if (deck.getCommandersJson() != null && !deck.getCommandersJson().isBlank()) {
            try {
                return MAPPER.readValue(deck.getCommandersJson(), new TypeReference<List<CommanderDTO>>() {});
            } catch (Exception ignored) {
                return List.of(new CommanderDTO(deck.getCommander(), "commander"));
            }
        }
        return List.of(new CommanderDTO(deck.getCommander(), "commander"));
    }

    private List<DeckHistoryEntryDTO> historyFor(Deck deck) {
        if (deck.getHistoryJson() == null || deck.getHistoryJson().isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(deck.getHistoryJson(), new TypeReference<List<DeckHistoryEntryDTO>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private RecommendationAuditExportDTO toAuditDto(RecommendationAuditRun audit) {
        return new RecommendationAuditExportDTO(
                audit.getId(),
                audit.getDeckId(),
                audit.getCommander(),
                audit.getColorIdentity(),
                audit.getBracket(),
                audit.getArchetype(),
                audit.getAlgorithmVersion(),
                audit.getCreatedAt(),
                audit.getGapsJson(),
                audit.getIssuesJson(),
                audit.getWeakCardsJson(),
                audit.getParamsJson(),
                audit.getRecommendationsJson(),
                audit.getBlockedPairsJson(),
                audit.getProtectedCutsJson(),
                audit.getFeedbackStatus(),
                audit.getFeedbackReason(),
                audit.getFeedbackNotes(),
                audit.getFeedbackAt()
        );
    }

    private void validateUser(AuthenticatedUserDTO user) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        validateOwner(user.googleSubject());
    }

    private void validateOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
    }
}
