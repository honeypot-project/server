package web;

import data.HoneypotDataRepo;
import domain.Challenge;
import domain.HoneypotUser;
import data.Repos;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import service.HoneypotService;
import util.HoneypotErrors;

import java.util.List;

public class ApiBridge {
  private static final HoneypotService service = Repos.service;
  public static final HoneypotDataRepo repo = Repos.dataRepo;

  public static void hello(RoutingContext routingContext) {
    Response.sendOkResponse(routingContext);
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
    Response.sendOkResponse(routingContext);
  }

  public static void uploadImg(RoutingContext routingContext) {
    // Check if user is logged in
    if (userIsNotLoggedIn(routingContext)) return;
    if (isUserDisabled(routingContext)) return;
    if (moreThanOneFileIsSubmitted(routingContext)) return;
    if (fileIsNotImage(routingContext)) return;
    if (imageIsSVG(routingContext)) return;

    // Check if picture was submitted
    List<FileUpload> files = routingContext.fileUploads();
    int userId = routingContext.session().get("id");
    service.uploadImg(userId, files);
    Response.sendOkResponse(routingContext);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void getUsers(RoutingContext routingContext) {
    if (userIsNotLoggedIn(routingContext)) return;
    if (userIsNotAdmin(routingContext)) return;
    if (isUserDisabled(routingContext)) return;
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
    Response.sendOkResponse(routingContext);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void getChallenges(RoutingContext routingContext) {
    if (userIsNotLoggedIn(routingContext)) return;

    int userId = routingContext.session().get("id");
    List<Challenge> solvedChallenges = service.getChallenges(userId);
    Response.sendJsonResponse(routingContext, 200, solvedChallenges);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void submitChallenge(RoutingContext routingContext) {
    if (userIsNotLoggedIn(routingContext)) return;
    if (isUserDisabled(routingContext)) return;

    Request request = Request.from(routingContext);
    String challengeId = request.getChallengeId();
    String flag = request.getFlag();
    int userId = routingContext.session().get("id");

    boolean success = service.submitChallenge(userId, challengeId, flag);
    if (!success) {
      Response.sendFailure(routingContext, 404, HoneypotErrors.SUBMIT_FAILED_ERROR);
    } else {
      Response.sendOkResponse(routingContext);
    }
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void toggleUser(RoutingContext routingContext) {
    if (userIsNotLoggedIn(routingContext)) return;
    if (userIsNotAdmin(routingContext)) return;
    if (isUserDisabled(routingContext)) return;

    Request request = Request.from(routingContext);
    int userToBeToggled = request.getUserId();

    service.toggleUser(userToBeToggled);
    Response.sendOkResponse(routingContext);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void getOnlineUsers(RoutingContext routingContext) {
    if (userIsNotLoggedIn(routingContext)) return;
    if (userIsNotAdmin(routingContext)) return;
    if (isUserDisabled(routingContext)) return;

    List<HoneypotUser> onlineUsers = service.getOnlineUsers();
    Response.sendJsonResponse(routingContext, 200, onlineUsers);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void getUser(RoutingContext routingContext) {
    if (userIsNotLoggedIn(routingContext)) return;
    int userId = routingContext.session().get("id");
    HoneypotUser user = repo.getUser(userId);
    Response.sendJsonResponse(routingContext, 200, user);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  public static void updateAdminRights(RoutingContext routingContext) {
    if (userIsNotLoggedIn(routingContext)) return;
    if (userIsNotAdmin(routingContext)) return;
    if (isUserDisabled(routingContext)) return;

    Request request = Request.from(routingContext);
    int userToMakeAdmin = request.getUserId();

    service.updateAdminRights(userToMakeAdmin);
    Response.sendOkResponse(routingContext);
    repo.updateLastAction(routingContext.session().get("id"));
  }

  // Helper methods
  private static boolean userIsNotAdmin(RoutingContext routingContext) {
    int userId = routingContext.session().get("id");
    if (!repo.isUserAdmin(userId)) {
      Response.sendFailure(routingContext, 403, "You are not an admin");
      return true;
    }
    return false;
  }

  private static boolean userIsNotLoggedIn(RoutingContext routingContext) {
    if (routingContext.session().get("id") == null) {
      Response.sendFailure(routingContext, 403, HoneypotErrors.USER_NOT_LOGGED_IN_ERROR);
      return true;
    }
    return false;
  }

  private static boolean isUserDisabled(RoutingContext routingContext) {
    int userId = routingContext.session().get("id");
    if (repo.getUser(userId).isDisabled()) {
      Response.sendFailure(routingContext, 403, "Your account has been disabled");
      return true;
    }
    return false;
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

  private static boolean fileIsNotImage(RoutingContext routingContext) {
    List<FileUpload> files = routingContext.fileUploads();
    if (!files.get(0).contentType().startsWith("image/")) {
      Response.sendFailure(routingContext, 400, "File is not an image");
      service.deleteFiles(files);
      return true;
    }
    return false;
  }

  private static boolean moreThanOneFileIsSubmitted(RoutingContext routingContext) {
    List<FileUpload> files = routingContext.fileUploads();
    if (files.size() != 1) {
      Response.sendFailure(routingContext, 400, HoneypotErrors.ONLY_ONE_FILE_ERROR);
      service.deleteFiles(files);
      return true;
    }
    return false;
  }


  public static void uploadImgFailure(RoutingContext routingContext) {
    Response.sendFailure(routingContext, 413, HoneypotErrors.UPLOAD_IMG_TOO_BIG);
  }
}
