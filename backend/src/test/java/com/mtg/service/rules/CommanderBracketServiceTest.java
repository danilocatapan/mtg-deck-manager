package com.mtg.service.rules;

import com.mtg.dto.BracketEstimateDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommanderBracketServiceTest {

    private final CommanderBracketService service = new CommanderBracketService();

    @Test
    void mapsLegacyAliasesToLevels() {
        assertEquals(2, service.levelFor("casual"));
        assertEquals(3, service.levelFor("mid"));
        assertEquals(4, service.levelFor("high-power"));
        assertEquals(5, service.levelFor("cedh"));
    }

    @Test
    void keepsNumericModelAvailable() {
        assertEquals("precon", service.normalizeAlias("1"));
        assertEquals("casual", service.normalizeAlias("2"));
        assertEquals("mid", service.normalizeAlias("3"));
        assertEquals("high-power", service.normalizeAlias("4"));
        assertEquals("cedh", service.normalizeAlias("5"));
    }

    @Test
    void preconLikeDeckWithoutGameChangersStaysLowBracket() {
        BracketEstimateDTO estimate = service.estimate(input(99, true, 3.8, 3, 5, 0, 0, 0, 0, 9));

        assertEquals(2, estimate.level());
        assertEquals("casual", estimate.alias());
    }

    @Test
    void oneToThreeGameChangersRaiseToUpgradedMinimum() {
        BracketEstimateDTO estimate = service.estimate(input(99, true, 3.4, 4, 6, 2, 0, 0, 0, 8));

        assertEquals(3, estimate.level());
        assertEquals("mid", estimate.alias());
    }

    @Test
    void fourGameChangersRaiseToHighPowerMinimum() {
        BracketEstimateDTO estimate = service.estimate(input(99, true, 3.2, 5, 7, 4, 0, 0, 0, 7));

        assertEquals(4, estimate.level());
        assertEquals("high-power", estimate.alias());
    }

    @Test
    void fastTutoredComboDeckRaisesToCedh() {
        BracketEstimateDTO estimate = service.estimate(input(99, true, 2.4, 12, 13, 4, 2, 3, 3, 4));

        assertEquals(5, estimate.level());
        assertEquals("cedh", estimate.alias());
    }

    @Test
    void illegalDeckRemainsExhibition() {
        BracketEstimateDTO estimate = service.estimate(input(99, false, 2.2, 20, 20, 10, 4, 5, 5, 3));

        assertEquals(1, estimate.level());
        assertEquals("precon", estimate.alias());
    }

    private CommanderBracketService.BracketEstimateInput input(
            int mainDeckSize,
            boolean legal,
            double averageCmc,
            int interaction,
            int ramp,
            int gameChangers,
            int comboDensity,
            int tutors,
            int fastMana,
            int winTurn
    ) {
        return new CommanderBracketService.BracketEstimateInput(
                mainDeckSize,
                legal,
                averageCmc,
                interaction,
                ramp,
                gameChangers,
                comboDensity,
                tutors,
                fastMana,
                0,
                0,
                winTurn,
                null
        );
    }
}
