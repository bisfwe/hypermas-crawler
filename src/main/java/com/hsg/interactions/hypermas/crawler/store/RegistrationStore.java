package com.hsg.interactions.hypermas.crawler.store;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

import java.util.Map;

public class RegistrationStore {

    private Vertx vertx;
    private LocalMap<String, String> subscriptions;


    public RegistrationStore() {
        vertx = Vertx.currentContext().owner();
        subscriptions = vertx.sharedData().getLocalMap("subscriptions");
    }

    public void addRegistration(String crawlUrl) {
        addRegistrationData(crawlUrl, "");
    }

    public void removeRegistration(String crawlUrl) {
        subscriptions.remove(crawlUrl);
    }

    public void addRegistrationData(String url, String representation) {
        if (subscriptions.containsKey(url) && !representation.equals("")) {
           subscriptions.replace(url, representation);
        } else {
            subscriptions.put(url, representation);
        }
    }

    public Map<String, String> getAllRegistrations() {
        return subscriptions;
    }

    public boolean contains(String url) {
        return subscriptions.containsKey(url);
    }
}

