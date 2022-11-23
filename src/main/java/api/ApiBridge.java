package api;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import service.HoneypotService;

import java.util.List;

public class ApiBridge {
  private static final HoneypotService service = new HoneypotService();
  private static final MySQLPool pool = MainVerticle.pool;


  public static void hello(RoutingContext routingContext) {
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("message", "ok"));
  }

  public static void register(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String username = request.getUsername();
    String password = request.getPassword();
    // Hash password

    service.addUser(routingContext, pool, username, password);
  }

  public static void uploadImg(RoutingContext routingContext) {
    // Check if picture was submitted
    List<FileUpload> files = routingContext.fileUploads();

    service.uploadImg(routingContext, pool, files);

  }

  public static void getUsers(RoutingContext routingContext) {
    service.getUsers(routingContext, pool);
  }

  public static void login(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String username = request.getUsername();
    String password = request.getPassword();

    service.login(routingContext, pool, username, password);
  }

  public static void getChallenges(RoutingContext routingContext) {
    service.getChallenges(routingContext, pool);
  }

  public static void submitChallenge(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String challengeId = request.getChallengeId();
    String flag = request.getFlag();

    service.submitChallenge(routingContext, pool, challengeId, flag);
  }

  public static void toggleUser(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String userId = request.getUserId();

    service.toggleUser(routingContext, pool, userId);
  }

  public static void getOnlineUsers(RoutingContext routingContext) {
    service.getOnlineUsers(routingContext, pool);
  }

  public static void getUser(RoutingContext routingContext) {
    service.getUser(routingContext, pool);
  }

  public static void makeAdmin(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String userId = request.getUserId();

    service.makeAdmin(routingContext, pool, userId);
  }
}
