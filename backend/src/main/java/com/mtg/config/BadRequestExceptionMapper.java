package com.mtg.config;

import com.mtg.dto.ErrorResponseDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Invalid request"
                : exception.getMessage();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponseDTO(message))
                .build();
    }
}
