package com.mtg.dto;

public class UserCollectionImportDTO {
    private String content;
    private boolean replaceExisting;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }
}
