package com.mtg.service.meta;

import java.util.List;

public class MetaCommander {
    private String commander;
    private List<String> colors;
    private List<MetaCard> cards;

    public MetaCommander() {}

    public String getCommander() { return commander; }
    public List<String> getColors() { return colors; }
    public List<MetaCard> getCards() { return cards; }
}
