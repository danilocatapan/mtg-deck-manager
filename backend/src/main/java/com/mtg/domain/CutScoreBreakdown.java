package com.mtg.domain;

import java.util.List;

public record CutScoreBreakdown(
        double cutScore,
        double lowSynergy,
        double highCmcLowImpact,
        double lowMetaFit,
        double roleRedundancy,
        double badMultiplayerScaling,
        double manaInefficiency,
        double strategicKeepValue,
        boolean protectedCut,
        List<String> reasons
) {
    public CutScoreBreakdown {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
