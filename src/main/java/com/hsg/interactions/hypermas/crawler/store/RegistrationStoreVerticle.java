package com.hsg.interactions.hypermas.crawler.store;

import com.google.gson.Gson;
import com.hsg.interactions.hypermas.crawler.core.EventBusMessage;
import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

import java.util.Optional;

public class RegistrationStoreVerticle extends AbstractVerticle {

    private RegistrationStore store;

    @Override
    public void start() {

        store = new RegistrationStore();

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(EventBusRegistry.REGISTRATION_STORE_ADDRESS, this::handleSubscription);
    }

    private void handleSubscription(Message<String> message) {
        EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);

        switch (request.getMessageType()) {
            case ADD_REGISTRATION:
                handleAddSubscription(request, message);
                break;
            case REMOVE_REGISTRATION:
                handleRemoveSubscription(request, message);
                break;
            case ADD_REGISTRATION_DATA:
                handleAddRegistrationData(request, message);
        }
    }

    private void handleAddSubscription(EventBusMessage request, Message<String> message) {
        Optional<String> url = request.getPayload();
        if (url.isPresent()) {
            store.addRegistration(url.get());
            message.reply("ok");
        }
    }

    private void handleRemoveSubscription(EventBusMessage request, Message<String> message) {
        Optional<String> url = request.getPayload();
        if (url.isPresent()) {
            store.removeRegistration(url.get());
            message.reply("ok");
        }
    }

    private void handleAddRegistrationData(EventBusMessage request, Message<String> message) {
        Optional<String> subscriptionUrl = request.getHeader(EventBusMessage.Headers.SUBSCRIPTION_URL);
        Optional<String> data = request.getPayload();
        if (subscriptionUrl.isPresent() && data.isPresent() && !store.getAllRegistrations().get(subscriptionUrl.get()).equals(data.get())) {
            store.addRegistrationData(subscriptionUrl.get(), data.get());
            message.reply("ok");
        }
    }
}
