import com.hsg.interactions.hypermas.crawler.core.CrawlerVerticle;
import com.hsg.interactions.hypermas.crawler.http.HttpServerVerticle;
import com.hsg.interactions.hypermas.crawler.store.LinkStoreVerticle;
import com.hsg.interactions.hypermas.crawler.store.RegistrationStoreVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() {
        vertx.deployVerticle(new HttpServerVerticle(),
                new DeploymentOptions().setWorker(true));

        vertx.deployVerticle(new RegistrationStoreVerticle(), new DeploymentOptions());

        vertx.deployVerticle(new CrawlerVerticle(), new DeploymentOptions());

        vertx.deployVerticle(new LinkStoreVerticle(), new DeploymentOptions());

    }
}
