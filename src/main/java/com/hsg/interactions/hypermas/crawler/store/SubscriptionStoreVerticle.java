package com.hsg.interactions.hypermas.crawler.store;

import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class SubscriptionStoreVerticle extends AbstractVerticle {

    private RegistrationStore store;

    @Override
    public void start() {

        store = new RegistrationStore();

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(EventBusRegistry.REGISTRATION_STORE_ADDRESS, this::handleAddSubscription);
    }

    private void handleAddSubscription(Message<String> message) {
        store.addRegistration(message.body());
        message.reply("ok");
    }
}
