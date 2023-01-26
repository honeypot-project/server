package web;

import data.MySQLConnection;
import data.Repos;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.SessionStore;
import util.HoneypotException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainVerticle extends AbstractVerticle {
  private static final Logger LOGGER = Logger.getLogger(MainVerticle.class.getName());
  private Promise<Void> startPromise;
  private static final Long KB = 1000L;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    this.startPromise = startPromise;

    // Test database connection and create startup tables
    try (Connection connection = MySQLConnection.getConnection()) {
      LOGGER.info("Database connection successful");
    } catch (SQLException ex ) {
      LOGGER.severe("Database connection couldn't be established");
      if (ex instanceof SQLSyntaxErrorException) {
        Repos.dataRepo.setup();
      } else {
        throw new SQLException(ex);
      }
    }

    // Create a router object.
    Router router = Router.router(vertx);

    // Create session handler
    SessionStore store = SessionStore.create(vertx);
    SessionHandler sessionHandler = SessionHandler.create(store);
    router.route().handler(sessionHandler);
    router.route().handler(createCorsHandler());

    router.get("/").handler(ApiBridge::hello);

    // Login
    router.post("/login").handler(BodyHandler.create()).handler(ApiBridge::login);

    // Register, option verb might not be needed, CSRF is confusing
    router.options("/register").handler(createCorsHandler()).handler(ApiBridge::hello);
    router.post("/register").handler(BodyHandler.create()).handler(ApiBridge::register);

    // Get user details
    router.get("/user").handler(ApiBridge::getUser);
    // Gets all users
    router.get("/users").handler(ApiBridge::getUsers);
    // Gets online users
    router.get("/online").handler(ApiBridge::getOnlineUsers);
    // This toggles the users' status (disabled/enabled)
    router.get("/toggleUser").handler(ApiBridge::toggleUser);
    router.get("/admin").handler(ApiBridge::updateAdminRights);

    // Image upload function
    router.post("/upload").handler(BodyHandler.create()
        .setUploadsDirectory("temp-uploads")
        .setHandleFileUploads(true)
        .setBodyLimit(10000L * KB))
      .failureHandler(ApiBridge::uploadImgFailure)
      .handler(ApiBridge::uploadImg);
    router.route("/uploads/images/*").handler(
      StaticHandler.create("uploads/images/").setCachingEnabled(true)
    );

    // Challenges
    router.get("/challenges").handler(ApiBridge::getChallenges);
    router.post("/challenges").handler(BodyHandler.create()).handler(ApiBridge::submitChallenge);

    // Start the web server and tell it to use the router to handle requests.

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onFailure(cause -> shutDown("Failed to start server", cause))
      .onSuccess(server -> {
        System.out.println("server started");
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
