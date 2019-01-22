package com.hsg.interactions.hypermas.crawler.store;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

import java.util.Map;

public class RegistrationStore {

    private Vertx vertx;
    private LocalMap<String, String> subscriptions;
    private LocalMap<String, Integer> nUpdates;
    private LocalMap<String, Long> lastCrawled;

    public RegistrationStore() {
        vertx = Vertx.currentContext().owner();
        // representation of subscriptions and crawled uris
        subscriptions = vertx.sharedData().getLocalMap("subscriptions");
        // timestamp of last crawling
        lastCrawled = vertx.sharedData().getLocalMap("lastCrawled");
        // number of updates of the representation of an uri
        nUpdates = vertx.sharedData().getLocalMap("nUpdates");
    }

    public void addRegistration(String crawlUrl) {
        addRegistrationData(crawlUrl, "");
    }

    public void removeRegistration(String crawlUrl) {
        subscriptions.remove(crawlUrl);
    }

    public void addRegistrationData(String url, String representation) {
        lastCrawled.put(url, System.currentTimeMillis());
        if (subscriptions.containsKey(url) && !representation.equals("")) {
           subscriptions.replace(url, representation);
           if (!subscriptions.get(url).equals(representation)) {
               if (nUpdates.containsKey(url)) {
                   nUpdates.replace(url, nUpdates.get(url) + 1);
               } else {
                   nUpdates.put(url, 1);
               }
           }
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

    public Map<String, Long> getLastCrawledInfo() {
        return lastCrawled;
    }
}

