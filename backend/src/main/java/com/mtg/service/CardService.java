package com.mtg.service;

import com.mtg.client.ScryfallClient;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ScryfallCardResponseDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CardService {

    private final ScryfallClient scryfallClient;

    @Inject
    public CardService(@RestClient ScryfallClient scryfallClient) {
        this.scryfallClient = scryfallClient;
    }

    public CardResponseDTO findByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The card name must be provided");
        }

        ScryfallCardResponseDTO response = scryfallClient.findByName(name.trim());
        if (response == null) {
            throw new IllegalStateException("Scryfall returned no card data");
        }

        return new CardResponseDTO(
                response.name(),
                response.cmc(),
                response.mana_cost(),
                response.type_line()
        );
    }
}
