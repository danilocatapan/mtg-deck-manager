package com.mtg.service.meta;

import java.util.List;

public class MetaCommander {
    private String commander;
    private List<String> colors;
    private List<MetaCard> cards;
    private String bracket;
    private List<String> sources;

    public MetaCommander() {}

    public String getCommander() { return commander; }
    public List<String> getColors() { return colors; }
    public List<MetaCard> getCards() { return cards == null ? List.of() : cards; }
    public String getBracket() { return bracket; }
    public List<String> getSources() { return sources == null ? List.of("LOCAL") : sources; }
}
