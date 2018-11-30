package com.hsg.interactions.hypermas.crawler.store;

import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import com.hsg.interactions.hypermas.crawler.store.SubscriptionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class SubscriptionStoreVerticle extends AbstractVerticle {

    private SubscriptionStore store;

    @Override
    public void start() {

        store = new SubscriptionStore();

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(EventBusRegistry.SUBSCRIPTION_STORE_ADDRESS, this::handleAddSubscription);
    }

    private void handleAddSubscription(Message<String> message) {
        store.addSubscription(message.body());
        message.reply("ok");
    }
}
