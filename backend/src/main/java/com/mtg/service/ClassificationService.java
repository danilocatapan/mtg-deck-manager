package com.mtg.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ClassificationService {

    private static final Logger LOG = Logger.getLogger(ClassificationService.class);

    public enum CardCategory {
        RAMP, DRAW, REMOVAL, OTHER
    }

    public CardCategory classify(String oracleText) {
        if (oracleText == null || oracleText.isBlank()) {
            return CardCategory.OTHER;
        }

        String norm = oracleText.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ");

        if (norm.contains("add ") || norm.contains("addc") || norm.contains("mana") || norm.contains("tap: add")) {
            LOG.debugv("classify: detected ramp by oracleText={0}", oracleText);
            return CardCategory.RAMP;
        }

        if (norm.contains("draw") || norm.contains("card draw")) {
            LOG.debugv("classify: detected draw by oracleText={0}", oracleText);
            return CardCategory.DRAW;
        }

        if (norm.contains("destroy") || norm.contains("exile") || norm.contains("bounce") || norm.contains("return to")) {
            LOG.debugv("classify: detected removal by oracleText={0}", oracleText);
            return CardCategory.REMOVAL;
        }

        return CardCategory.OTHER;
    }
}
