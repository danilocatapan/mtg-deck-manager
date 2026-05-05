package com.mtg.service.meta;

import java.util.List;

public interface MetaProvider {
    List<MetaCard> getTopCards(String commander);
}
