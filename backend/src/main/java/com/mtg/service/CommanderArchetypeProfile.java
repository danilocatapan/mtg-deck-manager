package com.mtg.service;

import java.util.Set;

record CommanderArchetypeProfile(
        String commanderName,
        Set<String> colors,
        String archetype,
        String plan,
        Set<String> commanderTags
) {
}
