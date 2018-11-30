package com.hsg.interactions.hypermas.crawler.store;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

import java.util.Map;

public class SubscriptionStore {

    private Vertx vertx;
    private LocalMap<String, String> subscriptions;


    public SubscriptionStore() {
        vertx = Vertx.currentContext().owner();
        subscriptions = vertx.sharedData().getLocalMap("subscriptions");
    }

    public void addSubscription(String crawlUrl) {
        subscriptions.put(crawlUrl, "");
    }

    public void removeSubscription(String crawlUrl) {
        subscriptions.remove(crawlUrl);
    }

    public void addSubscriptionData(String url, String representation) {
        subscriptions.replace(url, representation);
    }

    public Map<String, String> getAllSubscriptions() {
        return subscriptions;
    }


}
