package com.mtg.service.meta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BracketMetaPolicyTest {

    private final BracketMetaPolicy policy = new BracketMetaPolicy();

    @Test
    void normalizesRecommendationBracketAliasesToSimpleFourLevelModel() {
        assertEquals("casual", policy.normalizeBracket("1"));
        assertEquals("casual", policy.normalizeBracket("bracket 1"));
        assertEquals("casual", policy.normalizeBracket("precon"));
        assertEquals("mid", policy.normalizeBracket("2"));
        assertEquals("mid", policy.normalizeBracket("bracket 2"));
        assertEquals("high-power", policy.normalizeBracket("3"));
        assertEquals("high-power", policy.normalizeBracket("bracket 3"));
        assertEquals("cedh", policy.normalizeBracket("4"));
        assertEquals("cedh", policy.normalizeBracket("bracket 4"));
    }

    @Test
    void keepsNamedBracketsStableForExplicitUserIntent() {
        assertEquals("casual", policy.normalizeBracket("casual"));
        assertEquals("mid", policy.normalizeBracket("mid"));
        assertEquals("high-power", policy.normalizeBracket("high-power"));
        assertEquals("cedh", policy.normalizeBracket("cedh"));
    }
}
