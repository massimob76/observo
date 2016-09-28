package observoexample.api;

import observoexample.news.News;
import observoexample.services.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/news")
public class NewsApi {

    private static NewsService newsService;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsApi.class);

    public static void setNewsService(NewsService newsService) {
        NewsApi.newsService = newsService;
    }

    @GET
    @Path("/all")
    public Response getAllNews() {
        LOGGER.debug("called getAllNews...");
        List<News> newsList = newsService.getNewsObserver().getNewsReceivedSoFar();
        return Response.ok(newsList, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/latest")
    public Response getLatestNews() {
        LOGGER.debug("called getLatestNews...");
        News latest = newsService.getNewsObserver().getLatest();
        return Response.ok(latest, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/latest-second")
    public Response getLatestNewsSecond() {
        LOGGER.debug("called getLatestNewsSecond...");
        News latest = newsService.getNewsSecondObserver().getLatest();
        return Response.ok(latest, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @POST
    @Path("/publish")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishNews(News news) {
        LOGGER.debug("called publishNews...");
        newsService.publish(news);
        return Response.ok().build();
    }

}
