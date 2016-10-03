package observointegration;

import observointegration.api.NewsApi;
import observointegration.services.NewsService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class NewsServer {

    private static final int PORT = 8080;

    public void startServer() throws Exception {
        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");

        ServletHolder jerseyServlet = handler.addServlet(ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", NewsApi.class.getCanonicalName());

        NewsApi.setNewsService(new NewsService());

        Server server = new Server(PORT);
        server.setHandler(handler);
        server.start();
        server.dumpStdErr();
        server.join();
    }

    public static void main( String[] args ) throws Exception {
        new NewsServer().startServer();
    }
}
