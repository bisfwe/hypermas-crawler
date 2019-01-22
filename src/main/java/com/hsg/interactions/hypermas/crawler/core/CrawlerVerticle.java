package com.hsg.interactions.hypermas.crawler.core;

import com.hsg.interactions.hypermas.crawler.store.LinkStore;
import com.hsg.interactions.hypermas.crawler.store.RegistrationStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CrawlerVerticle extends AbstractVerticle {
    private RegistrationStore registrationStore;
    private LinkStore linkStore;
    private static Handler<Long> action;
    private HttpClient httpClient;
    private RDF4J rdfImpl;
    private String dataFileName = "crawlerData.ttl";
    private int crawlingInterval = 5;
    private ArrayList<String> urlsVisited = new ArrayList<String>();
    private boolean configuredLinksOnly = true;



    // IDEA: Let consumers focus on artifacts to be notified about changes in the crawled data..

    @Override
    public void start(Future<Void> fut) {
        rdfImpl = new RDF4J();
        httpClient = vertx.createHttpClient();
        registrationStore = new RegistrationStore();
        linkStore = new LinkStore();
        action = id -> {
            crawl();
            writeTtl();
            //System.out.println("Waiting for next crawl...");
            vertx.setTimer(TimeUnit.SECONDS.toMillis(crawlingInterval), action);
        };

        vertx.setTimer(TimeUnit.MILLISECONDS.toMillis(1), action);
    }

    private void crawl() {
        Map<String, String> registrations = registrationStore.getAllRegistrations();
        Map<String, Long> lastCrawled = registrationStore.getLastCrawledInfo();

        for (String url :registrations.keySet()) {
        // TODO: replace with intelligent metric to crawl fast changing areas more often -> nUpdates
        //    if (lastCrawled.get(url) < System.currentTimeMillis() - crawlingInterval*1000) {
                System.out.println("Crawling " + url);
                visitUrl(url);
         //   }
        }
        urlsVisited.clear();
    }

    private void visitUrl(String url) {
        if (!urlsVisited.contains(url)) {
            urlsVisited.add(url);
            httpClient.getAbs(url, new Handler<HttpClientResponse>() {


                @Override
                public void handle(HttpClientResponse httpClientResponse) {
                    httpClientResponse.bodyHandler(buffer -> {
                        if (httpClientResponse.statusCode() >= 400 || buffer.toString().equals("Not Found")) {
                            //System.out.println("Removing registration: " + url);
                            EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.REMOVE_REGISTRATION);
                            message.setPayload(url);
                            vertx.eventBus().send(EventBusRegistry.REGISTRATION_STORE_ADDRESS, message.toJson());
                            return;
                        }

                        // registrationStore turtle data
                        EventBusMessage message = new EventBusMessage(EventBusMessage.MessageType.ADD_REGISTRATION_DATA);
                        message.setHeader(EventBusMessage.Headers.SUBSCRIPTION_URL, url);
                        message.setPayload(buffer.toString());
                        vertx.eventBus().send(EventBusRegistry.REGISTRATION_STORE_ADDRESS, message.toJson());

                        // look for new links
                        ByteArrayInputStream in = new ByteArrayInputStream(buffer.toString().getBytes());
                        RDFFormat format = RDFFormat.TURTLE;
                        RDFParser rdfParser = Rio.createParser(format);
                        Model model = new LinkedHashModel();
                        rdfParser.setRDFHandler(new StatementCollector(model));
                        Set<String> foundLinks;
                        try {
                            // parse string to graph
                            rdfParser.parse(in, "");
                            Graph graph = rdfImpl.asGraph(model);
                            if (configuredLinksOnly) {
                                foundLinks = findLinks(graph);
                            } else {
                                foundLinks = findUris(graph);
                            }

                            for (String link : foundLinks) {
                                visitUrl(link);
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

    // TODO IDEA: recognise links that return promising representations
    private Set<String> findUris(Graph graph) {
        Set<String> result = new HashSet<>();

        for (Triple t : graph.iterate()) {
            try {
                String object = t.getObject().toString().replace("<", "").replace(">", "");
                // check for valid uri
                new URL(object).toURI();
                result.add(object);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private Set<String> findLinks(Graph graph) {
        Set<String> result = new HashSet<>();
        List<Triple> resultTriples = new ArrayList<>();
        Map<String, String> links = linkStore.getAllLinks();

        for (String link : links.keySet()) {
            String queryLink = link;
            if (!(links.get(link).equals("") || links.get(link) == null)) {
                String fullPrefix = links.get(link);
                String nsPrefix = fullPrefix.substring(0, fullPrefix.indexOf(":"));
                String prefix = fullPrefix.substring(fullPrefix.indexOf(":") + 1);
                queryLink = prefix + link.substring(link.indexOf(nsPrefix) + nsPrefix.length());
            }
            IRI queryIRI = rdfImpl.createIRI(queryLink);
            findTriplesByPredicate(graph, queryIRI).iterator().forEachRemaining(resultTriples::add);
        }
        for (Triple t : resultTriples) {
            result.add(t.getObject().toString());
        }
        return result;
    }

    private Iterable<Triple> findTriplesByPredicate(Graph graph, IRI iri) {
        return graph.iterate(null, iri, null);
    }

    private void writeTtl() {
        Map<String, String> dataMap = registrationStore.getAllRegistrations();
        if ( dataMap.size() > 0 ) {
            //System.out.println("Writing crawler data to " + dataFileName);
            Path path = Paths.get(dataFileName);
            try (BufferedWriter writer = Files.newBufferedWriter(path))
            {
                for (String data : dataMap.values()) {
                    writer.write(data);
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            vertx.eventBus().send(EventBusRegistry.SEARCH_ENGINE_DATA_ADDRESS, dataFileName, handleStoreReply());
        }
    }

    Handler<AsyncResult<Message<String>>> handleStoreReply() {
        return reply -> {
            if (reply.succeeded()) {
                //System.out.println("[Corese] Data reloaded");
            } else {
                //System.out.println("[Corese] Data reload failed");
            }
        };
    }
}
