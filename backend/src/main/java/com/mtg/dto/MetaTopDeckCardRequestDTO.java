package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaTopDeckCardRequestDTO {
    private String name;
    private int quantity;
    private String section;
    private String scryfallId;
    private String setCode;
    private String setName;
    private String collectorNumber;
    private String finish;
    private String imageUrl;

    public MetaTopDeckCardRequestDTO() {
    }

    public MetaTopDeckCardRequestDTO(String name, int quantity, String section) {
        this(name, quantity, section, null, null, null, null, null, null);
    }

    public MetaTopDeckCardRequestDTO(
            String name,
            int quantity,
            String section,
            String scryfallId,
            String setCode,
            String setName,
            String collectorNumber,
            String finish,
            String imageUrl
    ) {
        this.name = name;
        this.quantity = quantity;
        this.section = section;
        this.scryfallId = scryfallId;
        this.setCode = setCode;
        this.setName = setName;
        this.collectorNumber = collectorNumber;
        this.finish = finish;
        this.imageUrl = imageUrl;
    }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public int quantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String section() { return section; }
    public void setSection(String section) { this.section = section; }
    public String scryfallId() { return scryfallId; }
    public void setScryfallId(String scryfallId) { this.scryfallId = scryfallId; }
    public String setCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String setName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String collectorNumber() { return collectorNumber; }
    public void setCollectorNumber(String collectorNumber) { this.collectorNumber = collectorNumber; }
    public String finish() { return finish; }
    public void setFinish(String finish) { this.finish = finish; }
    public String imageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
