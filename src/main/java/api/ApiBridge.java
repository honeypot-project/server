package api;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.mysqlclient.MySQLPool;
import service.HoneypotService;

import java.util.List;

public class ApiBridge {
  private static final HoneypotService service = new HoneypotService();
  private static final MySQLPool pool = MainVerticle.pool;


  public static void hello(RoutingContext routingContext) {
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("message", "ok"));
  }

  public static void testBody(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String input = request.getTestBody();
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("you said", input));
  }

  public static void testPath(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String input = request.getTestRequestParams();
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("you said", input));
  }

  public static void register(RoutingContext routingContext) {
    System.out.println("Registering");
    Request request = Request.from(routingContext);
    String username = request.getUsername();
    String password = request.getPassword();
    // Hash password


    service.addUser(routingContext, pool, username, password);
  }

  public static void uploadImg(RoutingContext routingContext) {
    // Check if picture was submitted
    List<FileUpload> files = routingContext.fileUploads();

    System.out.println(files.size());
    for (FileUpload file : files) {
      System.out.println(file.fileName());
    }
  }

  public static void getUsers(RoutingContext routingContext) {
    service.getUsers(routingContext, pool);
  }

  public static void login(RoutingContext routingContext) {
    Session session = routingContext.session();

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

  public static void disableUser(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String userId = request.getUserId();

    service.disableUser(routingContext, pool, userId);
  }
}
