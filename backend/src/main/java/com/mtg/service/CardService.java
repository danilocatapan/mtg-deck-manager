package com.mtg.service;

import com.mtg.client.ScryfallClient;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ScryfallCardDTO;
import com.mtg.dto.ScryfallResponseDTO;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CardService {

    private static final Logger LOG = Logger.getLogger(CardService.class);

    @ConfigProperty(name = "scryfall.api.url")
    String scryfallApiUrl;

    private final ScryfallClient scryfallClient;

    @Inject
    public CardService(@RestClient ScryfallClient scryfallClient) {
        this.scryfallClient = scryfallClient;
    }

    @CacheResult(cacheName = "cards-by-name")
    public List<CardResponseDTO> searchByName(String name) {
        String normalizedName = validateName(name);
        String query = buildNameQuery(normalizedName);

        LOG.infov("event=cards.search.request name={0}", normalizedName);
        LOG.infov("event=scryfall.url base={0}", scryfallApiUrl);
        LOG.infov("event=scryfall.query value={0}", query);
        LOG.debugv("event=scryfall.search.start query={0}", query);

        try {
            ScryfallResponseDTO response = scryfallClient.searchByName(query);
            List<CardResponseDTO> cards = mapResponse(response);
            LOG.debugv("event=scryfall.search.success name={0} resultCount={1}", normalizedName, cards.size());
            return cards;
        } catch (NotFoundException exception) {
            LOG.debugv("event=scryfall.search.empty name={0}", normalizedName);
            return List.of();
        } catch (WebApplicationException | ProcessingException exception) {
            LOG.errorv(exception, "event=scryfall.search.failure name={0}", normalizedName);
            throw new ExternalServiceException("Failed to fetch cards from Scryfall", exception);
        }
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The card name must be provided");
        }

        return name.trim();
    }

    private String buildNameQuery(String name) {
        return "\"" + name.replace("\"", "\\\"") + "\"";
    }

    private List<CardResponseDTO> mapResponse(ScryfallResponseDTO response) {
        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data().stream()
                .map(this::toCardResponse)
                .toList();
    }

    private CardResponseDTO toCardResponse(ScryfallCardDTO card) {
        return new CardResponseDTO(
                card.name(),
                card.manaCost(),
                card.typeLine(),
                card.oracleText(),
                card.cmc()
        );
    }
}
