package com.hsg.interactions.hypermas.crawler.store;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hsg.interactions.hypermas.crawler.core.EventBusMessage;
import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class LinkStoreVerticle extends AbstractVerticle {

    private LinkStore store;
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkStoreVerticle.class.getName());


    @Override
    public void start() {

        store = new LinkStore();

        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(EventBusRegistry.CRAWL_LINK_STORE_ADDRESS, this::handleLink);

    }

    private void handleLink(Message<String> message) {
        EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);

        switch (request.getMessageType()) {
            case ADD_LINK_CRAWLER:
                handleAddLink(request, message);
                break;
            case GET_LINKS_CRAWLER:
                handleGetLinks(message);
                break;
            case DELETE_LINK_CRAWLER:
                handleRemoveLink(request, message);
                break;
            default:
                LOGGER.error("No handler registered for message with type " + request.getMessageType() + "!");
        }

    }

    private void handleAddLink(EventBusMessage request, Message<String> message) {
        Optional<String> payload = request.getPayload();
        if (payload.isPresent()) {
            Gson gson = new Gson();
            JsonElement element = gson.fromJson (payload.get(), JsonElement.class);
            JsonElement link = ((JsonObject) element).get("link");
            JsonElement prefix = ((JsonObject) element).get("prefix");
            if (link != null && prefix == null) {
                store.addLink(link.getAsString());
                message.reply("ok");
            } else if (link != null && prefix != null) {
                store.addLink(link.getAsString(), prefix.getAsString());
                message.reply("ok");
            }
        }
    }

    private void handleGetLinks(Message<String> message) {
        Map<String, String> result = store.getAllLinks();
        message.reply(Json.encode(result));
    }

    private void handleRemoveLink(EventBusMessage request, Message<String> message) {
        Optional<String> payload = request.getPayload();
        if (payload.isPresent()) {
            Gson gson = new Gson();
            JsonElement element = gson.fromJson (payload.get(), JsonElement.class);
            JsonElement link = ((JsonObject) element).get("link");
            if (link != null) {
                store.removeLink(link.getAsString());
            }
            message.reply(null);
        }
    }
}
