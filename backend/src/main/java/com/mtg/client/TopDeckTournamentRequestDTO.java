package com.mtg.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TopDeckTournamentRequestDTO {
    private String game;
    private String format;
    private int last;
    private int participantMin;
    private List<String> columns = new ArrayList<>();

    public TopDeckTournamentRequestDTO() {
    }

    public TopDeckTournamentRequestDTO(String game, String format, int last, int participantMin, List<String> columns) {
        this.game = game;
        this.format = format;
        this.last = last;
        this.participantMin = participantMin;
        this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
    }

    public String game() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String format() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int last() {
        return last;
    }

    public void setLast(int last) {
        this.last = last;
    }

    public int participantMin() {
        return participantMin;
    }

    public void setParticipantMin(int participantMin) {
        this.participantMin = participantMin;
    }

    public List<String> columns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
    }
}
