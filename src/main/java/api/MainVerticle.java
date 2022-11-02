package api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainVerticle extends AbstractVerticle {
  private static final Logger LOGGER = Logger.getLogger(MainVerticle.class.getName());
  private Promise<Void> startPromise;
  private static final int KB = 1000;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    this.startPromise = startPromise;
    Router router = Router.router(vertx);
    router.route().handler(createCorsHandler());
    router.route().handler(BodyHandler.create()
      .setUploadsDirectory("image-uploads")
      .setBodyLimit(500 * KB));

    router.get("/").handler(ApiBridge::hello);

    router.post("/test").handler(ApiBridge::testBody);
    router.get("/test").handler(ApiBridge::testPath);

    router.post("/addimage").handler(ApiBridge::addImage);
    router.route("/uploads/imgs/*").handler(
      StaticHandler.create("image-uploads").setCachingEnabled(false)
    );

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onFailure(cause -> shutDown("Failed to start server", cause))
      .onSuccess(server -> {
        LOGGER.log(Level.INFO, "Server is listening on port: {0}", server.actualPort());
        startPromise.complete();
      });
  }

  private void shutDown(String message, Throwable cause) {
    LOGGER.log(Level.SEVERE, message, cause);
    LOGGER.info("Shutting down");
    vertx.close();
    startPromise.fail(cause);
  }

  private CorsHandler createCorsHandler() {
    return CorsHandler.create(".*.")
      .allowedHeader("x-requested-with")
      .allowedHeader("Access-Control-Allow-Origin")
      .allowedHeader("Access-Control-Allow-Credentials")
      .allowCredentials(true)
      .allowedHeader("origin")
      .allowedHeader("Content-Type")
      .allowedHeader("Authorization")
      .allowedHeader("accept")
      .allowedMethod(HttpMethod.HEAD)
      .allowedMethod(HttpMethod.GET)
      .allowedMethod(HttpMethod.POST)
      .allowedMethod(HttpMethod.OPTIONS)
      .allowedMethod(HttpMethod.PATCH)
      .allowedMethod(HttpMethod.DELETE)
      .allowedMethod(HttpMethod.PUT);
  }
}
