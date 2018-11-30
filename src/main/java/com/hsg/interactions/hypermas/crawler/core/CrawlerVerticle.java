package com.hsg.interactions.hypermas.crawler.core;

import com.hsg.interactions.hypermas.crawler.store.SubscriptionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CrawlerVerticle extends AbstractVerticle {
    private SubscriptionStore store;
    private static Handler<Long> action;
    private HttpClient httpClient;
    private RDF4J rdfImpl;
    private Set<Graph> graphSet = new HashSet<>();


    @Override
    public void start(Future<Void> fut) {
        rdfImpl = new RDF4J();
        httpClient = vertx.createHttpClient();
        store = new SubscriptionStore();
        action = id -> {
            this.crawl();
            System.out.println("Wait for next crawl...");
            vertx.setTimer(TimeUnit.SECONDS.toMillis(5), action);
        };

        vertx.setTimer(TimeUnit.MILLISECONDS.toMillis(1), action);
    }

    private void crawl() {
        graphSet.clear();
        Map<String, Long> subscriptions = store.getAllSubscriptions();
        Set<String> newUrls = new HashSet<>();
        for (String url :subscriptions.keySet()) {
            httpClient.getAbs(url, new Handler<HttpClientResponse>() {

                @Override
                public void handle(HttpClientResponse httpClientResponse) {
                    httpClientResponse.bodyHandler(buffer -> {
                        if (buffer.toString().equals("Not Found")) {
                            return;
                        }
                        ByteArrayInputStream in = new ByteArrayInputStream(buffer.toString().getBytes());
                        RDFFormat format = RDFFormat.TURTLE;
                        RDFParser rdfParser = Rio.createParser(format);
                        Model model = new LinkedHashModel();
                        rdfParser.setRDFHandler(new StatementCollector(model));
                        try {
                            // parse string to graph
                            rdfParser.parse(in, "");
                            Graph graph = rdfImpl.asGraph(model);
                            graphSet.add(graph);

                            Set<String> foundLinks = findLinks(graph);
                            for (String link : foundLinks) {
                                store.addSubscription(link);
                            }
                        } catch (RDFParseException e) {
                            throw new IllegalArgumentException("RDF parse error: " + e.getMessage());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    System.out.println("Crawled url: " + url);
                }
            }).putHeader("Content-Type", "text/turtle").end();
        }
    }

    private Set<String> findLinks(Graph graph) {
        Set<String> result = new HashSet<>();

        // look for other links
        IRI linksIri = rdfImpl.createIRI("http://w3id.org/eve#links");
        IRI containsIri = rdfImpl.createIRI("http://w3id.org/eve#contains");
        Iterable<Triple> linkTriples = findTriplesByPredicate(graph, linksIri);
        for (Triple t : linkTriples) {
            result.add(t.getObject().toString());
        }
        Iterable<Triple> containsTriples = findTriplesByPredicate(graph, containsIri);
        for (Triple t : containsTriples) {
            result.add(t.getObject().toString());
        }
        return result;
    }

    private Iterable<Triple> findTriplesByPredicate(Graph graph, IRI iri) {
        return graph.iterate(null, iri, null);
    }
}
