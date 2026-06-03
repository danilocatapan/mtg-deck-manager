package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.mtg.model.DeckVisibility;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DeckImportDTO {
    private String name;
    private String commander;
    private String content;
    private List<CommanderDTO> commanders;
    private DeckVisibility visibility;
    private String sourceFormat;

    public DeckImportDTO() {
    }

    public DeckImportDTO(String name, String commander, String content) {
        this(name, commander, content, null, null, null);
    }

    public DeckImportDTO(String name, String commander, String content, List<CommanderDTO> commanders) {
        this(name, commander, content, commanders, null, null);
    }

    public DeckImportDTO(
            String name,
            String commander,
            String content,
            List<CommanderDTO> commanders,
            DeckVisibility visibility
    ) {
        this(name, commander, content, commanders, visibility, null);
    }

    public DeckImportDTO(
            String name,
            String commander,
            String content,
            List<CommanderDTO> commanders,
            DeckVisibility visibility,
            String sourceFormat
    ) {
        this.name = name;
        this.commander = commander;
        this.content = content;
        this.commanders = commanders;
        this.visibility = visibility;
        this.sourceFormat = sourceFormat;
    }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public String commander() { return commander; }
    public void setCommander(String commander) { this.commander = commander; }
    public String content() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<CommanderDTO> commanders() { return commanders; }
    public void setCommanders(List<CommanderDTO> commanders) { this.commanders = commanders; }
    public DeckVisibility visibility() { return visibility; }
    public void setVisibility(DeckVisibility visibility) { this.visibility = visibility; }
    public String sourceFormat() { return sourceFormat; }
    public void setSourceFormat(String sourceFormat) { this.sourceFormat = sourceFormat; }
}
