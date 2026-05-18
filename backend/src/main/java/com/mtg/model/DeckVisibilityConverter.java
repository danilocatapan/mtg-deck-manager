package com.mtg.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeckVisibilityConverter implements AttributeConverter<DeckVisibility, String> {

    @Override
    public String convertToDatabaseColumn(DeckVisibility visibility) {
        return (visibility == null ? DeckVisibility.PRIVATE : visibility).value();
    }

    @Override
    public DeckVisibility convertToEntityAttribute(String value) {
        return DeckVisibility.from(value);
    }
}
