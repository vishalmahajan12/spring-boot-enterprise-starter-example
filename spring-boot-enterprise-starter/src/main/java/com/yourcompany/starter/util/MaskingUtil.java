package com.yourcompany.starter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for masking sensitive data in logs.
 * 
 * This class provides functionality to:
 * 1. Mask sensitive fields in JSON payloads (password, token, etc.)
 * 2. Mask patterns like credit cards, SSN, emails in plain text
 * 3. Mask sensitive headers
 * 
 * Prevents sensitive information from appearing in logs.
 */
public class MaskingUtil {
    private static final String MASK_STRING = "****";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Regex patterns for common sensitive data patterns
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    /**
     * Masks sensitive data in a JSON string based on field names.
     * @param data JSON string to mask
     * @param sensitiveFields List of field names to mask
     * @return Masked JSON string
     */
    public static String maskSensitiveData(String data, List<String> sensitiveFields) {
        if (StringUtils.isBlank(data) || sensitiveFields == null || sensitiveFields.isEmpty()) {
            return data;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            maskJsonNode(jsonNode, sensitiveFields);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            // If not JSON, try regex masking for patterns
            return maskPatterns(data);
        }
    }

    /**
     * Recursively masks sensitive fields in a JSON node.
     */
    private static void maskJsonNode(JsonNode node, List<String> sensitiveFields) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                // Check if field name contains any sensitive field keyword
                if (sensitiveFields.stream().anyMatch(field -> 
                    fieldName.toLowerCase().contains(field.toLowerCase()))) {
                    objectNode.put(fieldName, MASK_STRING);
                } else if (objectNode.get(fieldName).isObject() || objectNode.get(fieldName).isArray()) {
                    // Recursively mask nested objects and arrays
                    maskJsonNode(objectNode.get(fieldName), sensitiveFields);
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> maskJsonNode(child, sensitiveFields));
        }
    }

    /**
     * Masks common patterns like credit cards, SSN, emails in plain text.
     */
    private static String maskPatterns(String data) {
        String masked = data;
        masked = CREDIT_CARD_PATTERN.matcher(masked).replaceAll(MASK_STRING);
        masked = SSN_PATTERN.matcher(masked).replaceAll(MASK_STRING);
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("***@***.***");
        return masked;
    }

    /**
     * Masks sensitive headers based on header names.
     * @param headers Map of headers
     * @param sensitiveFields List of sensitive field names
     * @return Map with masked headers
     */
    public static Map<String, String> maskHeaders(Map<String, String> headers, List<String> sensitiveFields) {
        if (headers == null) return headers;
        headers.forEach((key, value) -> {
            if (sensitiveFields.stream().anyMatch(field -> 
                key.toLowerCase().contains(field.toLowerCase()))) {
                headers.put(key, MASK_STRING);
            }
        });
        return headers;
    }
}

