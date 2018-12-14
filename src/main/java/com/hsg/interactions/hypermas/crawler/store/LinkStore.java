package com.hsg.interactions.hypermas.crawler.store;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

import java.util.Map;

public class LinkStore {

    private Vertx vertx;
    private LocalMap<String, String> crawlerLinks;


    public LinkStore() {
        vertx = Vertx.currentContext().owner();
        crawlerLinks = vertx.sharedData().getLocalMap("crawlLinks");
    }

    public void addLink(String link) {
        crawlerLinks.put(link, "");
    }

    public void removeLink(String link) {
        crawlerLinks.remove(link);
    }

    public void addLink(String link, String prefix) {
        crawlerLinks.put(link, prefix);
    }

    public Map<String, String> getAllLinks() {
        return crawlerLinks;
    }


}