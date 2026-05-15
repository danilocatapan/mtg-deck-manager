package com.mtg.config;

public final class SensitiveLogSanitizer {
    private SensitiveLogSanitizer() {}

    public static String reasonCode(String message) {
        if (message == null || message.isBlank()) {
            return "invalid_request";
        }

        String normalized = message.trim().toLowerCase();
        if (normalized.contains("not found")) {
            return "referenced_resource_not_found";
        }
        if (normalized.contains("required")) {
            return "required_field";
        }
        if (normalized.contains("too long")) {
            return "value_too_long";
        }
        if (normalized.contains("too large")) {
            return "payload_too_large";
        }
        if (normalized.contains("already exists")) {
            return "duplicate_value";
        }
        if (normalized.contains("between")) {
            return "invalid_range";
        }
        if (normalized.contains("maximum")) {
            return "limit_exceeded";
        }
        return "invalid_request";
    }
}
