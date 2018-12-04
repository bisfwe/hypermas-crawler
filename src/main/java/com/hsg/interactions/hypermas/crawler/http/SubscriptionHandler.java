package com.hsg.interactions.hypermas.crawler.http;

import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;


public class SubscriptionHandler {

    private Vertx vertx;

    public SubscriptionHandler() {
        vertx = Vertx.currentContext().owner();
    }

    public void handleAddSubscription(RoutingContext routingContext) {
        System.out.println("Add subscription");
        String crawlUrl = routingContext.getBodyAsString();

        vertx.eventBus().send(EventBusRegistry.SUBSCRIPTION_STORE_ADDRESS, crawlUrl, handleStoreReply(routingContext, HttpStatus.SC_OK));
    }

    public void handleRemoveSubscription(RoutingContext routingContext) {
        System.out.println("remove subscription");

    }

    public Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext, int succededStatusCode) {
        return reply -> {
            if (reply.succeeded()) {
                HttpServerResponse httpServerResponse = routingContext.response();
                httpServerResponse.setStatusCode(succededStatusCode);
                httpServerResponse.end();
            } else {
                routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        };
    }
}
