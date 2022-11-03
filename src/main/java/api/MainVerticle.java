package api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;

import java.util.logging.Level;
import java.util.logging.Logger;


public class MainVerticle extends AbstractVerticle {
  private static final Logger LOGGER = Logger.getLogger(MainVerticle.class.getName());
  private Promise<Void> startPromise;
  private static final int KB = 1000;

  public static MySQLPool pool;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    this.startPromise = startPromise;

    // Create a mysql client
    pool = MySQLPool.pool(vertx, new MySQLConnectOptions()
        .setPort(3306)
        .setHost("localhost")
        .setDatabase("honeypot")
        .setUser("root")
        .setPassword("123"),
      new PoolOptions().setMaxSize(5));

    // Create a router object.
    Router router = Router.router(vertx);

    // Create session handler
    SessionStore store = SessionStore.create(vertx);
    SessionHandler sessionHandler = SessionHandler.create(store);
    router.route().handler(sessionHandler);

    router.route().handler(createCorsHandler());
    router.route().handler(BodyHandler.create()
      .setUploadsDirectory("image-uploads")
      .setBodyLimit(500 * KB));

    router.get("/").handler(ApiBridge::hello);
    router.post("/test").handler(ApiBridge::testBody);
    router.get("/test").handler(ApiBridge::testPath);


    router.get("/challenges").handler(ApiBridge::getChallenges);
    router.post("/challenges").handler(ApiBridge::submitChallenge);

    // Register, option verb might not be needed, CSRF is confusing
    router.options("/register").handler(createCorsHandler()).handler(ApiBridge::hello);
    router.post("/register").handler(createCorsHandler()).handler(ApiBridge::register);

    router.post("/login").handler(ApiBridge::login);

    // Gets all users
    router.get("/users").handler(ApiBridge::getUsers);

    // This toggles the users status (disabled/enabled)
    router.get("/toggleUser").handler(ApiBridge::toggleUser);

    // Image upload function
    router.post("/upload").handler(ApiBridge::uploadImg);
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
    return CorsHandler.create()
      .allowedHeader("x-requested-with")
      .allowedHeader("Access-Control-Allow-Headers")
      .allowedHeader("Access-Control-Allow-Method")
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
