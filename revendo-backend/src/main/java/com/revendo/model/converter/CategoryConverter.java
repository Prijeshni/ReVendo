package com.revendo.model.converter;

import com.revendo.model.Listing.Category;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts between the {@link Category} enum and the DB string values
 * that contain spaces (e.g. "Home Appliances", "Household Goods").
 */
@Converter(autoApply = false)
public class CategoryConverter implements AttributeConverter<Category, String> {

    @Override
    public String convertToDatabaseColumn(Category category) {
        if (category == null) return null;
        return category.toString(); // "Home Appliances", "Household Goods", etc.
    }

    @Override
    public Category convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        // Replace spaces with underscores to match enum constant names
        return Category.valueOf(dbValue.replace(' ', '_'));
    }
}
