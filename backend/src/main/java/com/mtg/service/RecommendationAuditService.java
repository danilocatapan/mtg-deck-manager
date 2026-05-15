package com.mtg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.RecommendationAuditFeedbackDTO;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.model.Deck;
import com.mtg.model.RecommendationAuditRun;
import com.mtg.repository.RecommendationAuditRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RecommendationAuditService {
    private static final Logger LOG = Logger.getLogger(RecommendationAuditService.class);
    private static final String ALGORITHM_VERSION = "strategic-v1";
    private static final Set<String> FEEDBACK_STATUSES = Set.of("accepted", "rejected", "needs_review");

    @Inject
    RecommendationAuditRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public RecommendationAuditRun persistRun(
            Deck deck,
            String ownerId,
            RecommendationParamsDTO params,
            String bracket,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            StrategicDeckAssessment assessment,
            List<StrategicRecommendation> recommendations,
            RecommendationAuditContext context
    ) {
        RecommendationAuditRun run = new RecommendationAuditRun();
        run.setDeckId(deck == null ? null : deck.getId());
        run.setOwnerId(ownerId);
        run.setCommander(deck == null ? null : deck.getCommander());
        run.setColorIdentity(deck == null ? null : deck.getColorIdentity());
        run.setBracket(bracket);
        run.setArchetype(profile == null ? null : profile.archetype());
        run.setAlgorithmVersion(ALGORITHM_VERSION);
        run.setCreatedAt(OffsetDateTime.now());
        run.setGapsJson(toJson(roles == null ? Map.of() : roles.gaps()));
        run.setIssuesJson(toJson(assessment == null ? List.of() : assessment.issues()));
        run.setWeakCardsJson(toJson(assessment == null ? List.of() : assessment.weakCards()));
        run.setParamsJson(toJson(params == null ? Map.of() : params));
        run.setRecommendationsJson(toJson(recommendations == null ? List.of() : recommendations));
        run.setBlockedPairsJson(toJson(context == null ? List.of() : context.blockedPairs()));
        run.setProtectedCutsJson(toJson(context == null ? List.of() : context.protectedCuts()));

        repository.persist(run);
        LOG.infov(
                "event=recommendation.audit.persisted auditId={0} deckId={1} commander=\"{2}\" bracket={3} archetype={4} recommendations={5} blockedPairs={6} protectedCuts={7}",
                run.getId(),
                run.getDeckId(),
                run.getCommander(),
                run.getBracket(),
                run.getArchetype(),
                recommendations == null ? 0 : recommendations.size(),
                context == null ? 0 : context.blockedPairs().size(),
                context == null ? 0 : context.protectedCuts().size()
        );
        return run;
    }

    @Transactional
    public RecommendationAuditRun updateFeedback(Long id, RecommendationAuditFeedbackDTO dto, String ownerId) {
        if (dto == null || dto.status() == null || !FEEDBACK_STATUSES.contains(dto.status())) {
            throw new IllegalArgumentException("Feedback status must be accepted, rejected or needs_review");
        }
        RecommendationAuditRun run = repository.findByIdAndOwner(id, ownerId);
        if (run == null) {
            throw new NotFoundException("Recommendation audit not found");
        }
        run.setFeedbackStatus(dto.status());
        run.setFeedbackReason(trimToNull(dto.reason()));
        run.setFeedbackNotes(trimToNull(dto.notes()));
        run.setFeedbackAt(OffsetDateTime.now());
        LOG.infov(
                "event=recommendation.audit.feedback auditId={0} deckId={1} status={2} reason=\"{3}\"",
                run.getId(),
                run.getDeckId(),
                run.getFeedbackStatus(),
                run.getFeedbackReason()
        );
        return run;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            LOG.warnv(exception, "event=recommendation.audit.serialize_failed type={0}", value == null ? "null" : value.getClass().getSimpleName());
            return "[]";
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
