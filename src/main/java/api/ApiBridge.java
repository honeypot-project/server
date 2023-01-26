package api;

import data.HoneypotDataRepo;
import domain.HoneypotUser;
import data.Repos;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import service.HoneypotService;
import util.HoneypotErrors;

import java.util.List;

public class ApiBridge {
  private static final HoneypotService service = new HoneypotService();
  private static final MySQLPool pool = MainVerticle.pool;
  public static final HoneypotDataRepo repo = Repos.dataRepo;

  public static void hello(RoutingContext routingContext) {
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("message", "ok"));
  }

  public static void register(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String username = request.getUsername();
    String password = request.getPassword();
    HoneypotUser user = service.register(username, password);
    if (user == null) {
      Response.sendFailure(routingContext, 400, HoneypotErrors.REGISTER_FAILED_ERROR);
      return;
    }
    repo.addUser(user);
  }

  public static void uploadImg(RoutingContext routingContext) {
    // Check if user is logged in
    if (!isUserLoggedIn(routingContext)) return;
    if (!isUserDisabled(routingContext)) return;
    if (!onlyOneFileSubmitted(routingContext)) return;
    if (!isFileImage(routingContext)) return;
    if (!imageIsSVG(routingContext)) return;

    // Check if picture was submitted
    List<FileUpload> files = routingContext.fileUploads();
    String userID = routingContext.session().get("id");
    service.uploadImg(userID, files);
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("message", "ok"));
  }

  public static void getUsers(RoutingContext routingContext) {
    if (!isUserLoggedIn(routingContext)) return;
    if (!isUserAdmin(routingContext)) return;
    if (!isUserDisabled(routingContext)) return;
    List<HoneypotUser> users = repo.getUsers();
    Response.sendJsonResponse(routingContext, 200, users);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void login(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String username = request.getUsername();
    String password = request.getPassword();
    HoneypotUser user = service.login(username, password);
    if (user == null) {
      Response.sendFailure(routingContext, 400, HoneypotErrors.LOGIN_FAILED_ERROR);
      return;
    }
    routingContext.session().put("id", user.getId());
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("message", "ok"));
  }

  public static void getChallenges(RoutingContext routingContext) {
    service.getChallenges();
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
    String userId = routingContext.session().get("id");
    HoneypotUser user = repo.getUser(userId);
    Response.sendJsonResponse(routingContext, 200, user);
  }

  public static void makeAdmin(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String userId = request.getUserId();

    service.makeAdmin(routingContext, pool, userId);
  }

  private static boolean isUserAdmin(RoutingContext routingContext) {
    String userId = routingContext.session().get("id");
    if (!repo.isUserAdmin(userId)) {
      Response.sendFailure(routingContext, 403, "You are not an admin");
      return false;
    }
    return true;
  }

  private static boolean isUserLoggedIn(RoutingContext routingContext) {
    if (routingContext.session().get("id") == null) {
      Response.sendFailure(routingContext, 403, HoneypotErrors.USER_NOT_LOGGED_IN_ERROR);
      return false;
    }
    return true;
  }

  private static boolean isUserDisabled(RoutingContext routingContext) {
    String userId = routingContext.session().get("id");
    if (repo.getUser(userId).isDisabled()) {
      Response.sendFailure(routingContext, 403, "Your account has been disabled");
      return false;
    }
    return true;
  }


  private static boolean imageIsSVG(RoutingContext routingContext) {
    List<FileUpload> files = routingContext.fileUploads();
    if (files.get(0).contentType().equals("image/svg+xml")) {
      Response.sendFailure(routingContext, 400, "SVG files are not allowed");
      service.deleteFiles(files);
      return true;
    }
    return false;
  }

  private static boolean isFileImage(RoutingContext routingContext) {
    List<FileUpload> files = routingContext.fileUploads();
    if (!files.get(0).contentType().startsWith("image/")) {
      Response.sendFailure(routingContext, 400, "File is not an image");
      service.deleteFiles(files);
      return false;
    }
    return true;
  }

  private static boolean onlyOneFileSubmitted(RoutingContext routingContext) {
    List<FileUpload> files = routingContext.fileUploads();
    if (files.size() != 1) {
      Response.sendFailure(routingContext, 400, HoneypotErrors.ONLY_ONE_FILE_ERROR);
      service.deleteFiles(files);
      return false;
    }
    return true;
  }


}
