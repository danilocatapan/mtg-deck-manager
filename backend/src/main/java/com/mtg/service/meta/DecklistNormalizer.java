package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DecklistNormalizer {
    private static final Logger LOG = Logger.getLogger(DecklistNormalizer.class);
    private static final Pattern LINE = Pattern.compile("^\\s*(\\d+)\\s+(.+?)\\s*$");

    public List<MetaDeckCard> normalizePlainText(String decklist) {
        if (decklist == null || decklist.isBlank()) {
            return List.of();
        }
        List<MetaDeckCard> cards = new ArrayList<>();
        for (String rawLine : decklist.split("\\R")) {
            String line = sanitize(rawLine);
            if (line.isBlank() || isIgnoredLine(line)) {
                continue;
            }
            Matcher matcher = LINE.matcher(line);
            if (!matcher.matches()) {
                LOG.debugv("event=meta.normalize.skipped line={0}", line);
                continue;
            }
            int quantity = Integer.parseInt(matcher.group(1));
            String name = normalizeCardName(matcher.group(2));
            if (quantity > 0 && !name.isBlank()) {
                cards.add(new MetaDeckCard(name, quantity, List.of(), null, null, List.of()));
            }
        }
        LOG.infov("event=meta.normalize.completed cards={0}", cards.size());
        return cards;
    }

    public String findCommander(String decklist) {
        if (decklist == null || decklist.isBlank()) {
            return null;
        }

        boolean nextCardIsCommander = false;
        for (String rawLine : decklist.split("\\R")) {
            String line = sanitize(rawLine);
            if (line.isBlank() || line.startsWith("//") || line.startsWith("#")) {
                continue;
            }

            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.equals("commander") || lower.equals("commanders")) {
                nextCardIsCommander = true;
                continue;
            }
            if (lower.startsWith("commander:")) {
                String commander = parseCardName(line.substring(line.indexOf(':') + 1));
                if (commander != null) {
                    return commander;
                }
            }
            if (nextCardIsCommander) {
                String commander = parseCardName(line);
                if (commander != null) {
                    return commander;
                }
            }
        }

        return null;
    }

    private String sanitize(String line) {
        return line == null ? "" : line.trim();
    }

    private boolean isIgnoredLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.equals("commander")
                || lower.equals("commanders")
                || lower.equals("deck")
                || lower.equals("sideboard")
                || lower.equals("maybeboard")
                || lower.startsWith("//")
                || lower.startsWith("#");
    }

    private String parseCardName(String line) {
        Matcher matcher = LINE.matcher(sanitize(line));
        if (!matcher.matches()) {
            return null;
        }
        String name = normalizeCardName(matcher.group(2));
        return name.isBlank() ? null : name;
    }

    private String normalizeCardName(String value) {
        return value
                .replaceAll("\\s+\\([^)]*\\)", "")
                .replaceAll("\\s+\\[[^]]*]", "")
                .trim();
    }
}
