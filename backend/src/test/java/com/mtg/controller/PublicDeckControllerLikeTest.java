package com.mtg.controller;

import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.model.DeckLike;
import com.mtg.model.DeckVisibility;
import com.mtg.repository.DeckLikeRepository;
import com.mtg.repository.DeckRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class PublicDeckControllerLikeTest {

    @Inject
    DeckRepository deckRepository;

    @Inject
    DeckLikeRepository deckLikeRepository;

    @Test
    @TestSecurity(user = "like-user-1")
    void authenticatedUserCanLikePublicDeckOnce() {
        Long deckId = persistPublicDeck("Like Once Deck", "like-owner-1");

        given().when().post("/public/decks/" + deckId + "/like")
                .then()
                .statusCode(200)
                .body("likeCount", is(1))
                .body("likedByCurrentUser", is(true));

        given().when().post("/public/decks/" + deckId + "/like")
                .then()
                .statusCode(200)
                .body("likeCount", is(1))
                .body("likedByCurrentUser", is(true));
    }

    @Test
    void anonymousUserCannotLikePublicDeck() {
        Long deckId = persistPublicDeck("Anonymous Like Deck", "like-owner-2");

        given().when().post("/public/decks/" + deckId + "/like").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "like-user-2")
    void userRemovesOnlyOwnLike() {
        Long deckId = persistPublicDeck("Own Like Removal Deck", "like-owner-3");
        persistLike(deckId, "other-user", OffsetDateTime.now());

        given().when().post("/public/decks/" + deckId + "/like").then().statusCode(200).body("likeCount", is(2));
        given().when().delete("/public/decks/" + deckId + "/like").then().statusCode(204);

        given().when().get("/public/decks/" + deckId)
                .then()
                .statusCode(200)
                .body("likeCount", is(1))
                .body("likedByCurrentUser", is(false));
    }

    @Test
    @TestSecurity(user = "like-user-3")
    void privateDeckCannotBeLiked() {
        Long deckId = persistPrivateDeck("Private Like Deck", "private-like-owner");

        given().when().post("/public/decks/" + deckId + "/like").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "top-viewer")
    void topPublicDecksAreSortedByLikesInPeriod() {
        Long oneLikeDeck = persistPublicDeck("Top One Like Deck", "top-owner-1");
        Long twoLikeDeck = persistPublicDeck("Top Two Like Deck", "top-owner-2");
        Long oldLikeDeck = persistPublicDeck("Top Old Like Deck", "top-owner-3");
        persistLike(oneLikeDeck, "top-user-1", OffsetDateTime.now().minusDays(1));
        persistLike(twoLikeDeck, "top-user-2", OffsetDateTime.now().minusDays(1));
        persistLike(twoLikeDeck, "top-user-3", OffsetDateTime.now().minusDays(2));
        persistLike(oldLikeDeck, "top-user-4", OffsetDateTime.now().minusDays(40));

        given()
                .queryParam("period", "MONTHLY")
                .queryParam("size", 2)
                .when().get("/public/decks/top")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("name[0]", is("Top Two Like Deck"))
                .body("name", hasItem("Top One Like Deck"))
                .body("name", not(hasItem("Top Old Like Deck")));
    }

    private Long persistPublicDeck(String name, String ownerId) {
        return persistDeck(name, ownerId, DeckVisibility.PUBLIC);
    }

    private Long persistPrivateDeck(String name, String ownerId) {
        return persistDeck(name, ownerId, DeckVisibility.PRIVATE);
    }

    private Long persistDeck(String name, String ownerId, DeckVisibility visibility) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Deck deck = new Deck(name, "Cmd", List.of(new DeckCard("Sol Ring", 1)));
            deck.setOwnerId(ownerId);
            deck.setAuthorDisplayName("Like Tester");
            deck.setVisibility(visibility);
            deck.setColorIdentity("G");
            deckRepository.persist(deck);
            return deck.getId();
        });
    }

    private void persistLike(Long deckId, String ownerId, OffsetDateTime createdAt) {
        QuarkusTransaction.requiringNew().run(() -> {
            Deck deck = deckRepository.findById(deckId);
            DeckLike like = new DeckLike();
            like.setDeck(deck);
            like.setOwnerId(ownerId);
            like.setCreatedAt(createdAt);
            deckLikeRepository.persist(like);
        });
    }
}
