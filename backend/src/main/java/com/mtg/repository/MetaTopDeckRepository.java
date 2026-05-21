package com.mtg.repository;

import com.mtg.model.MetaDeckArchetype;
import com.mtg.model.MetaDeckBracket;
import com.mtg.model.MetaDeckFormat;
import com.mtg.model.MetaDeckSource;
import com.mtg.model.MetaRankingPeriod;
import com.mtg.model.MetaTopDeck;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class MetaTopDeckRepository implements PanacheRepository<MetaTopDeck> {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    public MetaTopDeck findSameSnapshotByDeckUrl(MetaDeckSource source, MetaRankingPeriod period, LocalDate rankingDate, String deckUrl) {
        if (deckUrl == null || deckUrl.isBlank()) {
            return null;
        }
        return find(
                "source = ?1 and rankingPeriod = ?2 and rankingDate = ?3 and lower(deckUrl) = ?4",
                source,
                period,
                rankingDate,
                deckUrl.trim().toLowerCase(Locale.ROOT)
        ).firstResult();
    }

    public MetaTopDeck findSameSnapshotByRank(
            MetaDeckSource source,
            MetaRankingPeriod period,
            LocalDate rankingDate,
            MetaDeckFormat format,
            String commanderNormalized,
            int rank
    ) {
        return find(
                "source = ?1 and rankingPeriod = ?2 and rankingDate = ?3 and format = ?4 and commanderNormalized = ?5 and rank = ?6",
                source,
                period,
                rankingDate,
                format,
                commanderNormalized,
                rank
        ).firstResult();
    }

    public List<MetaTopDeck> listFiltered(
            MetaDeckSource source,
            MetaRankingPeriod rankingPeriod,
            LocalDate rankingDate,
            MetaDeckFormat format,
            String commander,
            MetaDeckArchetype archetype,
            MetaDeckBracket bracket,
            String colorIdentity,
            Integer limit
    ) {
        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (source != null) {
            query.append(" and source = :source");
            params.put("source", source);
        }
        if (rankingPeriod != null) {
            query.append(" and rankingPeriod = :rankingPeriod");
            params.put("rankingPeriod", rankingPeriod);
        }
        if (rankingDate != null) {
            query.append(" and rankingDate = :rankingDate");
            params.put("rankingDate", rankingDate);
        }
        if (format != null) {
            query.append(" and format = :format");
            params.put("format", format);
        }
        if (commander != null && !commander.isBlank()) {
            query.append(" and commanderNormalized like :commander");
            params.put("commander", "%" + commander.trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (archetype != null) {
            query.append(" and archetype = :archetype");
            params.put("archetype", archetype);
        }
        if (bracket != null) {
            query.append(" and bracket = :bracket");
            params.put("bracket", bracket);
        }
        if (colorIdentity != null && !colorIdentity.isBlank()) {
            query.append(" and colorIdentity = :colorIdentity");
            params.put("colorIdentity", colorIdentity.trim().toUpperCase(Locale.ROOT));
        }
        query.append(" order by rankingDate desc, rank asc, id desc");
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        return find(query.toString(), params).page(Page.ofSize(safeLimit)).list();
    }

    public List<MetaTopDeck> listUsableForProfiles() {
        return list("bracket <> ?1 order by rankingDate desc, rank asc", MetaDeckBracket.UNKNOWN);
    }
}
