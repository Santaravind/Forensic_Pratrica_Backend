package com.security.forecsic.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.forecsic.model.BlogImage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class BlogImageListConverter implements AttributeConverter<List<BlogImage>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<BlogImage> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Error converting List<BlogImage> to JSON", e);
            return "[]";
        }
    }

    @Override
    public List<BlogImage> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<BlogImage>>() {});
        } catch (Exception e) {
            log.error("Error converting JSON to List<BlogImage>", e);
            return new ArrayList<>();
        }
    }
}
