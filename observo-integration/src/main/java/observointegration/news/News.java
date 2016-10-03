package observointegration.news;

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

    public static News generateNewsForTesting() {
        return new News("title " + System.currentTimeMillis(), "content");
    }

    @Override
    public String toString() {
        return "News{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof News)) return false;

        News news = (News) o;

        if (title != null ? !title.equals(news.title) : news.title != null) return false;
        return content != null ? content.equals(news.content) : news.content == null;

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }
}
