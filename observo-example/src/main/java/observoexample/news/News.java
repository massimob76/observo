package observoexample.news;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class News implements Serializable {

    private final String title;
    private final String content;

    public News(@JsonProperty("title") String title, @JsonProperty("content") String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "News{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
