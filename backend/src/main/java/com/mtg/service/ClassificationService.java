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

        String norm = oracleText.toLowerCase();

        if (norm.contains("add {") || norm.contains("add ")) {
            LOG.debugv("classify: detected ramp by oracleText={0}", oracleText);
            return CardCategory.RAMP;
        }

        if (norm.contains("draw")) {
            LOG.debugv("classify: detected draw by oracleText={0}", oracleText);
            return CardCategory.DRAW;
        }

        if (norm.contains("destroy") || norm.contains("exile")) {
            LOG.debugv("classify: detected removal by oracleText={0}", oracleText);
            return CardCategory.REMOVAL;
        }

        return CardCategory.OTHER;
    }
}
