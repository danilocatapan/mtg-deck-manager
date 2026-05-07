package com.mtg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jboss.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StructuredRestLog {
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private StructuredRestLog() {}

    public static void requestReceived(Logger logger, String requestId, String method, String endpoint, String contentType, String clientIp) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("contentType", contentType);
        details.put("clientIp", clientIp);
        info(logger, requestId, method, endpoint, null, "Requisicao recebida para processamento.", details);
    }

    public static void requestCompleted(Logger logger, String requestId, String method, String endpoint, int status, long durationMs) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("durationMs", durationMs);
        info(logger, requestId, method, endpoint, status, messageForStatus(status), details);
    }

    public static void validation(Logger logger, int status, String message, String rule, String field, String reason) {
        RequestLogContext.RequestInfo info = RequestLogContext.get();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("regra", rule);
        details.put("campo", field);
        details.put("motivo", reason);
        warn(
                logger,
                info == null ? null : info.requestId(),
                info == null ? null : info.method(),
                info == null ? null : info.endpoint(),
                status,
                message,
                details
        );
    }

    public static void exception(Logger logger, Throwable exception, int status, String message) {
        RequestLogContext.RequestInfo info = RequestLogContext.get();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("exceptionClass", exception.getClass().getName());
        details.put("errorMessage", exception.getMessage());
        details.put("cause", exception.getCause() == null ? null : exception.getCause().getMessage());
        details.put("stackTrace", stackTrace(exception));
        error(
                logger,
                info == null ? null : info.requestId(),
                info == null ? null : info.method(),
                info == null ? null : info.endpoint(),
                status,
                message,
                details
        );
    }

    private static void info(Logger logger, String requestId, String method, String endpoint, Integer status, String message, Map<String, Object> details) {
        logger.info(toJson("INFO", requestId, method, endpoint, status, message, details));
    }

    private static void warn(Logger logger, String requestId, String method, String endpoint, Integer status, String message, Map<String, Object> details) {
        logger.warn(toJson("WARN", requestId, method, endpoint, status, message, details));
    }

    private static void error(Logger logger, String requestId, String method, String endpoint, Integer status, String message, Map<String, Object> details) {
        logger.error(toJson("ERROR", requestId, method, endpoint, status, message, details));
    }

    private static String toJson(String level, String requestId, String method, String endpoint, Integer status, String message, Map<String, Object> details) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        event.put("level", level);
        event.put("requestId", requestId);
        event.put("method", method);
        event.put("endpoint", endpoint);
        event.put("status", status);
        event.put("mensagem", message);
        event.put("detalhes", details == null ? Map.of() : details);
        try {
            return MAPPER.writeValueAsString(event);
        } catch (Exception ignored) {
            return event.toString();
        }
    }

    private static String messageForStatus(int status) {
        if (status >= 500) {
            return "Requisicao concluida com erro interno.";
        }
        if (status >= 400) {
            return "Requisicao concluida com erro do cliente.";
        }
        return "Requisicao concluida com sucesso.";
    }

    private static String stackTrace(Throwable exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
