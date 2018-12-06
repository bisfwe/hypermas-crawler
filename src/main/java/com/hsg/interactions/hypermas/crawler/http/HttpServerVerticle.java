package com.hsg.interactions.hypermas.crawler.http;

import com.hsg.interactions.hypermas.crawler.search.SearchEngine;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.http.HttpStatus;

public class HttpServerVerticle extends AbstractVerticle {

    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = 9090;

    @Override
    public void start(Future<Void> fut) {
        io.vertx.core.http.HttpServer server = vertx.createHttpServer();

        String host = DEFAULT_HOST;
        int port  = DEFAULT_PORT;

        Router router = createRouter();
        server.requestHandler(router::accept).listen(port, host);
    }

    private Router createRouter() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.get("/").handler((routingContext) -> {
            routingContext.response()
                    .setStatusCode(HttpStatus.SC_OK)
                    .end("Crawler Yggdrasil -> Corese");
        });

        RegistrationHandler subHandler = new RegistrationHandler();
        SearchEngine searchHandler = new SearchEngine();

        router.post("/crawler/subscriptions").handler(subHandler::handleAddSubscription);
        // TODO post with body or get with path parameters?
        // atm body seems to be nicer, since sparql query encoded in path can be cumbersome
        router.post("/searchEngine").handler(searchHandler::handleSearchQuery);

        return router;
    }
}
