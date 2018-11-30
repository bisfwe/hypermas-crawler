package com.hsg.interactions.hypermas.crawler.store;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

import java.util.Date;
import java.util.Map;

public class SubscriptionStore {

    private Vertx vertx;
    private LocalMap<String, Long> subscriptions;


    public SubscriptionStore() {
        vertx = Vertx.currentContext().owner();
        subscriptions = vertx.sharedData().getLocalMap("subscriptions");
    }

    public void addSubscription(String crawlUrl) {
        subscriptions.put(crawlUrl, new Date().getTime());
    }

    public Map<String, Long> getAllSubscriptions() {
        return subscriptions;
    }
}
