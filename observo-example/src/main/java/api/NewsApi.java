package api;

import news.News;
import services.NewsService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/news")
public class NewsApi {

    private static NewsService newsManager = new NewsService();

    @GET
    @Path("/all")
    public Response getAllNews() {
        List<News> newsList = newsManager.getNewsObserver().getNewsReceivedSoFar();
        return Response.ok(newsList, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/latest")
    public Response getLatestNews() {
        News latest = newsManager.getNewsObserver().getLatest();
        return Response.ok(latest, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @POST
    @Path("/publish")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishNews(News news) {
        newsManager.publish(news);
        return Response.ok().build();
    }

}
