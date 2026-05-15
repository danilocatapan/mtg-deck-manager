package com.mtg.dto;

public record AuthenticatedUserDTO(
        String googleSubject,
        String email,
        String name,
        String avatarUrl
) {
}
