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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CrawlerVerticle extends AbstractVerticle {
    private SubscriptionStore store;
    private static Handler<Long> action;
    private HttpClient httpClient;
    private RDF4J rdfImpl;

    @Override
    public void start(Future<Void> fut) {
        rdfImpl = new RDF4J();
        httpClient = vertx.createHttpClient();
        store = new SubscriptionStore();
        action = id -> {
            crawl();
            System.out.println("Writing to ttl data file...");
            writeTtl();
            System.out.println("Waiting for next crawl...");
            vertx.setTimer(TimeUnit.SECONDS.toMillis(5), action);
        };

        vertx.setTimer(TimeUnit.MILLISECONDS.toMillis(1), action);
    }

    private void crawl() {
        Map<String, String> subscriptions = store.getAllSubscriptions();

        for (String url :subscriptions.keySet()) {
            httpClient.getAbs(url, new Handler<HttpClientResponse>() {

                @Override
                public void handle(HttpClientResponse httpClientResponse) {
                    httpClientResponse.bodyHandler(buffer -> {
                        if (buffer.toString().equals("Not Found")) {
                            System.out.println("Removing subscription: " + url);
                            store.removeSubscription(url);
                            return;
                        }
                        // store turtle data
                        if (!buffer.toString().equals(subscriptions.get(url))) {
                            store.addSubscriptionData(url, buffer.toString());
                        }
                        // look for new links
                        ByteArrayInputStream in = new ByteArrayInputStream(buffer.toString().getBytes());
                        RDFFormat format = RDFFormat.TURTLE;
                        RDFParser rdfParser = Rio.createParser(format);
                        Model model = new LinkedHashModel();
                        rdfParser.setRDFHandler(new StatementCollector(model));
                        try {
                            // parse string to graph
                            rdfParser.parse(in, "");
                            Graph graph = rdfImpl.asGraph(model);

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

    private void writeTtl() {
        Map<String, String> dataMap = store.getAllSubscriptions();
        Path path = Paths.get("crawlerData.ttl");
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            for (String data : dataMap.values()) {
                writer.write(data);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
