package com.hsg.interactions.hypermas.crawler.search;

import com.hsg.interactions.hypermas.crawler.core.EventBusRegistry;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;

import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.print.TripleFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.exceptions.EngineException;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.RoutingContext;

public class SearchEngine {
    private Vertx vertx;
    private Graph coreseGraph;
    private Load coreseLoad;
    private QueryProcess exec;

    public SearchEngine() {
        vertx = Vertx.currentContext().owner();
        coreseGraph = Graph.create();
        coreseLoad = Load.create(coreseGraph);
        exec = QueryProcess.create(coreseGraph);

        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(EventBusRegistry.SEARCH_ENGINE_DATA_ADDRESS, this::handleDataReload);
    }

    public void handleSearchQuery(RoutingContext routingContext) {
        try {
            String query = routingContext.getBodyAsString();
            Mappings map = exec.query(query);
            if (!query.contains("construct")) {
                // we can only return plain sparql results, no nice turtle
                ResultFormat f = ResultFormat.create(map);
                routingContext.response().putHeader("Content-Type", "application/sparql-results+xml").end(f.toString());
            } else {
                Graph g = exec.getGraph(map);
                TripleFormat tf = TripleFormat.create(g);
                routingContext.response().putHeader("Content-Type", "text/turtle").end(tf.toString());
            }

        } catch (EngineException e) {
            e.printStackTrace();
            routingContext.response().setStatusCode(400).end(e.getMessage());
        }
    }

    private void handleDataReload(Message<String> message) {
        String filePath = message.body();
        try {
            coreseGraph.empty();
            coreseLoad.parse(filePath);
            message.reply("ok");
        } catch (LoadException e) {
            e.printStackTrace();
            message.fail(500, e.getMessage());
        }
    }
}
