package com.revendo.model.converter;

import com.revendo.model.Listing.Condition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts between the {@link Condition} enum and the DB string values
 * that contain spaces (e.g. "Like New").
 */
@Converter(autoApply = false)
public class ConditionConverter implements AttributeConverter<Condition, String> {

    @Override
    public String convertToDatabaseColumn(Condition condition) {
        if (condition == null) return null;
        return condition.toString(); // "Like New", "New", etc.
    }

    @Override
    public Condition convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return Condition.valueOf(dbValue.replace(' ', '_'));
    }
}
