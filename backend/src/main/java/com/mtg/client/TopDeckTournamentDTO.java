package com.mtg.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TopDeckTournamentDTO {
    @JsonAlias({"TID", "id"})
    private String id;
    private String tournamentName;
    private String format;
    @JsonAlias({"participantCount", "participants", "players"})
    private Integer participantCount;
    private Long startDate;
    private String topdeckUrl;
    private List<TopDeckStandingDTO> standings;

    public TopDeckTournamentDTO() {
    }

    public TopDeckTournamentDTO(
            String id,
            String tournamentName,
            String format,
            Integer participantCount,
            Long startDate,
            String topdeckUrl,
            List<TopDeckStandingDTO> standings
    ) {
        this.id = id;
        this.tournamentName = tournamentName;
        this.format = format;
        this.participantCount = participantCount;
        this.startDate = startDate;
        this.topdeckUrl = topdeckUrl;
        this.standings = standings;
    }

    public String id() { return id; }
    public void setId(String id) { this.id = id; }
    public String tournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }
    public String format() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Integer participantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }
    public Long startDate() { return startDate; }
    public void setStartDate(Long startDate) { this.startDate = startDate; }
    public String topdeckUrl() { return topdeckUrl; }
    public void setTopdeckUrl(String topdeckUrl) { this.topdeckUrl = topdeckUrl; }
    public List<TopDeckStandingDTO> standings() { return standings; }
    public void setStandings(List<TopDeckStandingDTO> standings) { this.standings = standings; }
}
