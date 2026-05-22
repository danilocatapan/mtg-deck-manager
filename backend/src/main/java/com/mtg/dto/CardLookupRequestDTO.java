package com.mtg.dto;

public record CardLookupRequestDTO(
        String name,
        String setCode,
        String collectorNumber,
        String scryfallId
) {
    public CardLookupRequestDTO(String name) {
        this(name, null, null, null);
    }

    public boolean hasPrinting() {
        return setCode != null && !setCode.isBlank() && collectorNumber != null && !collectorNumber.isBlank();
    }

    public String lookupKey() {
        if (scryfallId != null && !scryfallId.isBlank()) {
            return "id:" + scryfallId.trim().toLowerCase(java.util.Locale.ROOT);
        }
        if (hasPrinting()) {
            return printingKey(setCode, collectorNumber);
        }
        return nameKey(name);
    }

    public static String nameKey(String name) {
        return name == null ? "" : "name:" + name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static String printingKey(String setCode, String collectorNumber) {
        String set = setCode == null ? "" : setCode.trim().toLowerCase(java.util.Locale.ROOT);
        String number = collectorNumber == null ? "" : collectorNumber.trim().toLowerCase(java.util.Locale.ROOT);
        return "print:" + set + ":" + number;
    }
}
