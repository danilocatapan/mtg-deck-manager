package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.LocalDate;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaTopDeckSyncRequestDTO {
    private String source;
    private String rankingPeriod;
    private LocalDate rankingDate;
    private Integer limitPerGroup;
    private String groupBy;
    private List<String> commanders;
    private List<String> archetypes;
    private List<String> brackets;

    public MetaTopDeckSyncRequestDTO() {
    }

    public MetaTopDeckSyncRequestDTO(
            String source,
            String rankingPeriod,
            LocalDate rankingDate,
            Integer limitPerGroup,
            String groupBy,
            List<String> commanders,
            List<String> archetypes,
            List<String> brackets
    ) {
        this.source = source;
        this.rankingPeriod = rankingPeriod;
        this.rankingDate = rankingDate;
        this.limitPerGroup = limitPerGroup;
        this.groupBy = groupBy;
        this.commanders = commanders;
        this.archetypes = archetypes;
        this.brackets = brackets;
    }

    public String source() { return source; }
    public void setSource(String source) { this.source = source; }
    public String rankingPeriod() { return rankingPeriod; }
    public void setRankingPeriod(String rankingPeriod) { this.rankingPeriod = rankingPeriod; }
    public LocalDate rankingDate() { return rankingDate; }
    public void setRankingDate(LocalDate rankingDate) { this.rankingDate = rankingDate; }
    public Integer limitPerGroup() { return limitPerGroup; }
    public void setLimitPerGroup(Integer limitPerGroup) { this.limitPerGroup = limitPerGroup; }
    public String groupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    public List<String> commanders() { return commanders; }
    public void setCommanders(List<String> commanders) { this.commanders = commanders; }
    public List<String> archetypes() { return archetypes; }
    public void setArchetypes(List<String> archetypes) { this.archetypes = archetypes; }
    public List<String> brackets() { return brackets; }
    public void setBrackets(List<String> brackets) { this.brackets = brackets; }
}
