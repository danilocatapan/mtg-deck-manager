package com.mtg.config;

import com.mtg.dto.ErrorResponseDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {
    private static final Logger LOG = Logger.getLogger(IllegalStateExceptionMapper.class);

    @Override
    public Response toResponse(IllegalStateException exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Invalid request state"
                : exception.getMessage();
        StructuredRestLog.validation(
                LOG,
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Requisicao rejeitada por violacao de regra de negocio.",
                "BUSINESS_RULE",
                null,
                message
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponseDTO(message))
                .build();
    }
}
