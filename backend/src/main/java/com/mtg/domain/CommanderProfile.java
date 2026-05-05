package com.mtg.domain;

import java.util.List;

public record CommanderProfile(
        String name,
        List<String> colors,
        List<String> tags
) {}
