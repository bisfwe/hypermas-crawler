package com.hsg.interactions.hypermas.crawler.search;

import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchVerticle extends AbstractVerticle {
    private Vertx vertx;
    private Graph coreseGraph;
    private Load coreseLoad;

    @Override
    public void start(Future<Void> fut) {
        vertx = Vertx.currentContext().owner();
        coreseGraph = Graph.create();
        coreseLoad = Load.create(coreseGraph);

        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(EventBusRegistry.SEARCH_ENGINE_ADDRESS, this::handleDataReload);
    }


    private void handleDataReload(Message<String> message) {
        String filePath = message.body();
        try {
            coreseGraph.empty();
            Path path = Paths.get("crawlerData.ttl");
            coreseLoad.parse(filePath);
            message.reply("ok");
        } catch (LoadException e) {
            e.printStackTrace();
            message.fail(500, e.getMessage());
        }
    }
}
