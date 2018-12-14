package com.hsg.interactions.hypermas.crawler.http;

import com.hsg.interactions.hypermas.crawler.core.EventBusMessage;
import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import io.netty.util.concurrent.SucceededFuture;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;


public class HttpHandler {

    private Vertx vertx;

    public HttpHandler() {
        vertx = Vertx.currentContext().owner();
    }

    public void handleAddRegistration(RoutingContext routingContext) {
        System.out.println("Add subscription");
        String crawlUrl = routingContext.getBodyAsString();

        EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.ADD_REGISTRATION);
        message.setPayload(crawlUrl);

        vertx.eventBus().send(EventBusRegistry.REGISTRATION_STORE_ADDRESS, message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_OK));
    }

    public void handleAddLink(RoutingContext routingContext) {
        System.out.println("Add hypermedia link to be crawled for");
        String crawlLink = routingContext.getBodyAsString();

        EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.ADD_LINK_CRAWLER);
        message.setPayload(crawlLink);

        vertx.eventBus().send(EventBusRegistry.CRAWL_LINK_STORE_ADDRESS, message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_OK));
    }

    public void handleGetLinks(RoutingContext routingContext) {
        EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.GET_LINKS_CRAWLER);
        vertx.eventBus().send(EventBusRegistry.CRAWL_LINK_STORE_ADDRESS, message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_OK));
    }

    public void handleRemoveLinks(RoutingContext routingContext) {
        EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.DELETE_LINK_CRAWLER);
        String deleteLink = routingContext.getBodyAsString();
        message.setPayload(deleteLink);
        vertx.eventBus().send(EventBusRegistry.CRAWL_LINK_STORE_ADDRESS, message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_OK));
    }

    public Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext, int succeededStatusCode) {
        return reply -> {
            if (reply.succeeded()) {
                HttpServerResponse httpServerResponse = routingContext.response();
                httpServerResponse.setStatusCode(succeededStatusCode);
                if( ! (reply.result().body() == null) && reply.result().body().length() > 0 ) {
                    httpServerResponse.putHeader("Content-Length", String.valueOf(reply.result().body().length()));
                    httpServerResponse.write(reply.result().body());
                }
                httpServerResponse.end();
            } else {
                routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        };
    }
}
