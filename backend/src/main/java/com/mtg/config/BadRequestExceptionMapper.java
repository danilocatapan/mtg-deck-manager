package com.mtg.config;

import com.mtg.dto.ErrorResponseDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    private static final Logger LOG = Logger.getLogger(BadRequestExceptionMapper.class);

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Invalid request"
                : exception.getMessage();
        StructuredRestLog.validation(
                LOG,
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Requisicao rejeitada por violacao de regra de negocio.",
                "INVALID_REQUEST",
                null,
                SensitiveLogSanitizer.reasonCode(message)
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponseDTO(message))
                .build();
    }
}
