package com.hsg.interactions.hypermas.crawler.http;

import com.hsg.interactions.hypermas.crawler.core.EventBusMessage;
import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;


public class RegistrationHandler {

    private Vertx vertx;

    public RegistrationHandler() {
        vertx = Vertx.currentContext().owner();
    }

    public void handleAddSubscription(RoutingContext routingContext) {
        System.out.println("Add subscription");
        String crawlUrl = routingContext.getBodyAsString();

        EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.ADD_REGISTRATION);
        message.setPayload(crawlUrl);

        vertx.eventBus().send(EventBusRegistry.REGISTRATION_STORE_ADDRESS, message.toJson(), handleStoreReply(routingContext, HttpStatus.SC_OK));
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
