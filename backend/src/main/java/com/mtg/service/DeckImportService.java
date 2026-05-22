package com.mtg.service;

import com.mtg.model.DeckCard;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DeckImportService {

    private static final int MAX_IMPORT_LINES = 120;
    private static final int MAX_LINE_LENGTH = 160;
    private static final Pattern LINE = Pattern.compile("^\\s*(\\d+)\\s*x?\\s+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOIL_TAG = Pattern.compile("\\s+\\*(F|E)\\*\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRINTING_WITH_NUMBER = Pattern.compile("^(.*?)\\s+\\(([A-Za-z0-9]{2,8})\\)\\s+([A-Za-z0-9-]+)\\s*$");
    private static final Pattern PRINTING_WITH_SET = Pattern.compile("^(.*?)\\s+\\(([A-Za-z0-9]{2,8})\\)\\s*$");

    public List<DeckCard> parse(String content) {
        return parse(content, null);
    }

    public List<DeckCard> parse(String content, String sourceFormat) {
        if (content == null || content.isBlank()) return List.of();

        String[] lines = content.split("\n");
        if (lines.length > MAX_IMPORT_LINES) {
            throw new IllegalArgumentException("Deck import accepts at most " + MAX_IMPORT_LINES + " lines");
        }

        List<DeckCard> cards = new ArrayList<>();
        boolean allowPrintingMetadata = allowsPrintingMetadata(sourceFormat);
        boolean ignoreSection = false;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            Section section = sectionFor(line);
            if (section == Section.IGNORED) {
                ignoreSection = true;
                continue;
            }
            if (section == Section.MAIN) {
                ignoreSection = false;
                continue;
            }
            if (isIgnoredLine(line) || ignoreSection) {
                continue;
            }
            validateLineLength(line);
            cards.add(parseLine(line, allowPrintingMetadata));
        }
        return cards;
    }

    private void validateLineLength(String line) {
        if (line.length() > MAX_LINE_LENGTH) {
            throw new IllegalArgumentException("Deck import line is too long");
        }
    }

    private DeckCard parseLine(String line, boolean allowPrintingMetadata) {
        try {
            Matcher lineMatcher = LINE.matcher(line);
            if (!lineMatcher.matches()) {
                throw new IllegalArgumentException("Invalid deck import line");
            }
            int quantity = Integer.parseInt(lineMatcher.group(1));
            ParsedCardName parsed = parseCardName(lineMatcher.group(2), allowPrintingMetadata);
            String name = parsed.name();
            if (quantity < 1 || quantity > 99) {
                throw new IllegalArgumentException("Invalid card quantity");
            }
            if (name.isBlank()) {
                throw new IllegalArgumentException("Card name is required");
            }
            return new DeckCard(name, quantity, parsed.setCode(), parsed.collectorNumber(), parsed.finish());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid deck import line");
        }
    }

    private boolean isIgnoredLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.equals("deck")
                || lower.equals("main deck")
                || lower.equals("mainboard")
                || lower.equals("commander")
                || lower.equals("commanders")
                || lower.startsWith("//")
                || lower.startsWith("#")
                || lower.startsWith("sb:");
    }

    private Section sectionFor(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.equals("sideboard") || lower.equals("maybeboard") || lower.equals("tokens") || lower.equals("considering")) {
            return Section.IGNORED;
        }
        if (lower.equals("deck") || lower.equals("main deck") || lower.equals("mainboard")) {
            return Section.MAIN;
        }
        return Section.NONE;
    }

    private ParsedCardName parseCardName(String value, boolean allowPrintingMetadata) {
        String finish = "UNKNOWN";
        String text = value == null ? "" : value.trim();
        if (!allowPrintingMetadata) {
            return new ParsedCardName(text, null, null, finish);
        }
        Matcher foilMatcher = FOIL_TAG.matcher(text);
        if (foilMatcher.find()) {
            finish = "F".equalsIgnoreCase(foilMatcher.group(1)) ? "FOIL" : "ETCHED";
            text = foilMatcher.replaceFirst("").trim();
        }

        String setCode = null;
        String collectorNumber = null;
        Matcher numbered = PRINTING_WITH_NUMBER.matcher(text);
        if (numbered.matches()) {
            text = numbered.group(1).trim();
            setCode = numbered.group(2).trim().toUpperCase(Locale.ROOT);
            collectorNumber = numbered.group(3).trim();
        } else {
            Matcher setOnly = PRINTING_WITH_SET.matcher(text);
            if (setOnly.matches()) {
                text = setOnly.group(1).trim();
                setCode = setOnly.group(2).trim().toUpperCase(Locale.ROOT);
            }
        }

        return new ParsedCardName(text, setCode, collectorNumber, finish);
    }

    private boolean allowsPrintingMetadata(String sourceFormat) {
        if (sourceFormat == null || sourceFormat.isBlank()) {
            return true;
        }
        String normalized = sourceFormat.trim().toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        return normalized.equals("MOXFIELD")
                || normalized.equals("MTG_ARENA")
                || normalized.equals("ARENA")
                || normalized.equals("ARCHIDEKT")
                || normalized.equals("GENERIC");
    }

    private record ParsedCardName(String name, String setCode, String collectorNumber, String finish) {
    }

    private enum Section {
        MAIN,
        IGNORED,
        NONE
    }
}
