package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class CardRoleClassifier {
    private static final Logger LOG = Logger.getLogger(CardRoleClassifier.class);

    public Set<String> rolesFor(CardResponseDTO card) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        String oracle = text(card == null ? null : card.oracleText());
        String type = primaryType(text(card == null ? null : card.typeLine()));
        Double cmc = card == null ? null : card.cmc();

        if (type.contains("land")) roles.add("land");
        if (isRamp(oracle, type)) roles.add("ramp");
        if (oracle.contains("draw")) roles.add("draw");
        if (oracle.contains("look at the top") || oracle.contains("scry") || oracle.contains("surveil")) roles.add("selection");
        if (oracle.contains("counter target")) roles.add("counterspell");
        if (oracle.contains("destroy") || oracle.contains("exile target") || oracle.contains("return target")) roles.add("removal");
        if (oracle.contains("hexproof") || oracle.contains("indestructible") || oracle.contains("phase out") || oracle.contains("protection")) roles.add("protection");
        if (isTutor(oracle)) roles.add("tutor");
        if (isStax(oracle)) roles.add("stax");
        if (isComboPiece(oracle)) roles.add("combo-piece");
        if (isFinisher(oracle, type, cmc)) roles.add("finisher");
        if (roles.isEmpty()) roles.add("value");

        if (LOG.isDebugEnabled() && card != null && card.name() != null) {
            LOG.debugv("event=card.role.classified card=\"{0}\" roles={1}", card.name(), roles);
        }
        return roles;
    }

    public String primaryRole(CardResponseDTO card) {
        Set<String> roles = rolesFor(card);
        for (String preferred : List.of("land", "protection", "draw", "counterspell", "removal", "ramp", "combo-piece", "tutor", "stax", "finisher")) {
            if (roles.contains(preferred)) {
                return "counterspell".equals(preferred) ? "removal" : preferred;
            }
        }
        return roles.iterator().next();
    }

    private boolean isRamp(String oracle, String type) {
        if (type.contains("land")) {
            return false;
        }
        return oracle.contains("add ")
                || oracle.contains("search your library for a land")
                || oracle.contains("put a land card")
                || oracle.contains("cost") && oracle.contains("less to cast")
                || oracle.contains("rather than pay");
    }

    private boolean isTutor(String oracle) {
        return oracle.contains("search your library")
                && !oracle.contains("basic land")
                && !oracle.contains("land card")
                && !oracle.contains("forest card");
    }

    private boolean isStax(String oracle) {
        return oracle.contains("can't cast")
                || oracle.contains("can't activate")
                || oracle.contains("enter tapped")
                || oracle.contains("skip their untap")
                || oracle.contains("spells cost");
    }

    private boolean isComboPiece(String oracle) {
        return oracle.contains("win the game")
                || oracle.contains("escape")
                || oracle.contains("storm")
                || oracle.contains("additional combat")
                || oracle.contains("extra combat")
                || oracle.contains("untap all attacking creatures")
                || oracle.contains("untap target")
                || oracle.contains("copy target activated ability");
    }

    private boolean isFinisher(String oracle, String type, Double cmc) {
        double value = cmc == null ? 0.0 : cmc;
        return oracle.contains("win the game")
                || oracle.contains("loses the game")
                || oracle.contains("extra turn")
                || oracle.contains("additional combat")
                || oracle.contains("extra combat")
                || oracle.contains("infect")
                || (type.contains("creature") && value >= 5.0 && (oracle.contains("trample") || oracle.contains("double")));
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String primaryType(String typeLine) {
        return typeLine.split("\\s+//\\s+", 2)[0];
    }
}
