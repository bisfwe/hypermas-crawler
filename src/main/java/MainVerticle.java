import com.hsg.interactions.hypermas.crawler.core.CrawlerVerticle;
import com.hsg.interactions.hypermas.crawler.http.HttpServerVerticle;
import com.hsg.interactions.hypermas.crawler.store.SubscriptionStoreVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() {
        vertx.deployVerticle(new HttpServerVerticle(),
                new DeploymentOptions().setWorker(true));

        vertx.deployVerticle(new SubscriptionStoreVerticle(), new DeploymentOptions());

        vertx.deployVerticle(new CrawlerVerticle(), new DeploymentOptions());
    }
}
