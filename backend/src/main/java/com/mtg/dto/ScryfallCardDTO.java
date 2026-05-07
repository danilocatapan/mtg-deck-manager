package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ScryfallCard")
public record ScryfallCardDTO(
        String name,
        @JsonProperty("mana_cost") String manaCost,
        @JsonProperty("cmc") Double cmc,
        @JsonProperty("type_line") String typeLine,
        @JsonProperty("oracle_text") String oracleText,
        @JsonProperty("color_identity") java.util.List<String> colorIdentity,
        @JsonProperty("image_uris") ImageUris imageUris,
        @JsonProperty("card_faces") java.util.List<CardFaceDTO> cardFaces,
        PricesDTO prices
) {
    public ScryfallCardDTO(
            String name,
            String manaCost,
            Double cmc,
            String typeLine,
            String oracleText,
            java.util.List<String> colorIdentity
    ) {
        this(name, manaCost, cmc, typeLine, oracleText, colorIdentity, null, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageUris(
            String small,
            String normal,
            String large,
            String png
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardFaceDTO(
            @JsonProperty("image_uris") ImageUris imageUris
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricesDTO(
            String usd,
            @JsonProperty("usd_foil") String usdFoil,
            String eur,
            String tix
    ) {
    }
}

