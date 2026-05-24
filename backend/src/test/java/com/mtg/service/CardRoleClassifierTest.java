package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardRoleClassifierTest {

    CardRoleClassifier classifier = new CardRoleClassifier();

    @Test
    void classifiesCoreCommanderRoles() {
        assertTrue(classifier.rolesFor(card("Sol Ring", "Artifact", "{T}: Add {C}{C}.", 1.0)).contains("ramp"));
        assertTrue(classifier.rolesFor(card("Demonic Tutor", "Sorcery", "Search your library for a card, put that card into your hand.", 2.0)).contains("tutor"));
        assertTrue(classifier.rolesFor(card("Counterspell", "Instant", "Counter target spell.", 2.0)).contains("counterspell"));
        assertTrue(classifier.rolesFor(card("Mystic Remora", "Enchantment", "Whenever an opponent casts a noncreature spell, you may draw a card.", 1.0)).contains("draw"));
        assertTrue(classifier.rolesFor(card("Rule of Law", "Enchantment", "Each player can't cast more than one spell each turn.", 3.0)).contains("stax"));
        assertTrue(classifier.rolesFor(card("Thassa's Oracle", "Creature", "If X is greater than or equal to the number of cards in your library, you win the game.", 2.0)).contains("combo-piece"));
        assertEquals("value", classifier.primaryRole(card("Grizzly Bears", "Creature", "", 2.0)));
    }

    @Test
    void doesNotClassifyLandsOrNonLandFrontFaceMdfcAsRampOrLandByBackFace() {
        assertEquals("land", classifier.primaryRole(card("Swamp", "Basic Land - Swamp", "Add {B}.", 0.0)));
        assertTrue(classifier.rolesFor(card("Swamp", "Basic Land - Swamp", "Add {B}.", 0.0)).contains("land"));
        assertFalse(classifier.rolesFor(card("Swamp", "Basic Land - Swamp", "Add {B}.", 0.0)).contains("ramp"));

        var agadeemRoles = classifier.rolesFor(card(
                "Agadeem's Awakening",
                "Sorcery // Land",
                "Return from your graveyard to the battlefield any number of target creature cards.",
                3.0
        ));
        assertFalse(agadeemRoles.contains("land"));
        assertFalse(agadeemRoles.contains("ramp"));
    }

    private CardResponseDTO card(String name, String type, String oracle, Double cmc) {
        return new CardResponseDTO(name, "", type, oracle, cmc, List.of(), List.of());
    }
}
