package com.fallguys.recruitment.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StatusConverter implements AttributeConverter<Status, String> {

    @Override
    public String convertToDatabaseColumn(Status attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case ACTIVE -> "OPEN";
            case DELETED -> "CLOSED";
        };
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "CLOSED" -> Status.DELETED;
            case "OPEN", "IN_PROGRESS", "COMPLETED" -> Status.ACTIVE;
            default -> throw new IllegalArgumentException("Unknown status value: " + dbData);
        };
    }
}
