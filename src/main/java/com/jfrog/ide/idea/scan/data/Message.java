package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Message {

    @JsonProperty("text")
    private String text;

    @JsonProperty("markdown")
    private String markdown;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Message)) {
            return false;
        }
        Message rhs = ((Message) other);
        return (Objects.equals(this.text, rhs.text));
    }

    @SuppressWarnings({"unused"})
    public String getMarkdown() {
        return markdown;
    }

    @SuppressWarnings({"unused"})
    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }
}
