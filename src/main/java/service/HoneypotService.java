package service;

import api.Response;
import com.password4j.Hash;
import com.password4j.Password;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class HoneypotService {
  private static final String USERNAME_TAKEN_ERROR = "user already exists";
  private static final String LOGIN_FAIL_ERROR = "user does not exist or wrong password";
  private static final String NOT_LOGGED_IN_ERROR = "not logged in";
  private static final String USER_DISABLED_ERROR = "user disabled";
  private static final String USER_NOT_ADMIN_ERROR = "user not admin";
  private static final String USER_SESSION_NOT_FOUND_ERROR = "please login again";
  private static final String USER_NOT_FOUND_ERROR = "user not found";
  private static final String USERNAME_OR_PASSWORD_EMPTY_ERROR = "username or password can not be empty";
  private static final String LOGIN_SUCCESSFUL = "login successful";
  private static final String SQL_SELECT_USER_BY_ID = "SELECT * FROM users WHERE id = ?";


  public void getUsers(RoutingContext routingContext, MySQLPool pool) {
    String id = routingContext.session().get("id");

    if (id == null) {
      Response.sendJsonResponse(routingContext, 401, NOT_LOGGED_IN_ERROR);
      return;
    }

    pool.preparedQuery(SQL_SELECT_USER_BY_ID)
      .execute(Tuple.of(id))
      .onSuccess(result -> {
        if (result.size() == 0) {

          Response.sendFailure(routingContext, 404, USER_SESSION_NOT_FOUND_ERROR);

        } else if (result.iterator().next().getBoolean("disabled")) {

          Response.sendFailure(routingContext, 403, USER_DISABLED_ERROR);

        } else if (!result.iterator().next().getBoolean("administrator")) {

          Response.sendFailure(routingContext, 403, USER_NOT_ADMIN_ERROR);

        } else {
          // Update last action on database
          pool.preparedQuery("UPDATE users SET last_action = NOW() WHERE id = ?")
            .execute(Tuple.of(id));

          // Get all users from DB
          pool.query("SELECT * FROM users")
            .execute()
            .onSuccess(users -> {
              JsonObject response = new JsonObject();
              for (Row row : users) {
                response.put(row.getInteger("id").toString(), new JsonObject()
                  .put("username", row.getString("username"))
                  .put("disabled", row.getBoolean("disabled"))
                  .put("admin", row.getBoolean("administrator")));
              }
              Response.sendJsonResponse(routingContext, 200, response);
            });
        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  public void addUser(RoutingContext routingContext, MySQLPool pool, String username, String password) {

    // Check if username and password is not empty
    Boolean usernameEmpty = username == null || username.isEmpty();
    Boolean passwordEmpty = password == null || password.isEmpty();
    if (usernameEmpty || passwordEmpty) {
      Response.sendFailure(routingContext, 400, USERNAME_OR_PASSWORD_EMPTY_ERROR);
      return;
    }

    // Validate username
    if (!username.matches("^[a-zA-Z0-9]+$")) {
      Response.sendFailure(routingContext, 400, "username can only contain letters and numbers");
      return;
    }

    // Lowercase username
    final String validatedUsername = username.toLowerCase();

    // Check if username is taken
    pool.preparedQuery("SELECT * FROM users WHERE username = ?")
      .execute(Tuple.of(validatedUsername))
      .onSuccess(rows -> {
        if (rows.size() != 0) {
          Response.sendFailure(routingContext, 400, USERNAME_TAKEN_ERROR);
        } else {

          // Hash password
          Hash hash = Password.hash(password).addRandomSalt().withArgon2();

          // Upload username and hashed password to database
          pool.preparedQuery("INSERT INTO users (username, password) VALUES (?, ?)")
            .execute(Tuple.of(validatedUsername, hash.getResult()))
            .onSuccess(res -> Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "user added")));
        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  public void login(RoutingContext routingContext, MySQLPool pool, String username, String password) {
    // Check if username and password is not empty
    Boolean usernameEmpty = username == null || username.isEmpty();
    Boolean passwordEmpty = password == null || password.isEmpty();
    if (usernameEmpty || passwordEmpty) {
      Response.sendFailure(routingContext, 400, USERNAME_OR_PASSWORD_EMPTY_ERROR);
      return;
    }

    // Validate username
    if (!username.matches("^[a-zA-Z0-9]+$")) {
      Response.sendFailure(routingContext, 400, "username can only contain letters and numbers");
      return;
    }

    // Lowercase username
    final String validatedUsername = username.toLowerCase();

    // Check if user exists
    pool.preparedQuery("SELECT * FROM users WHERE username = ?")
      .execute(Tuple.of(validatedUsername))
      .onSuccess(rows -> {
        if (rows.size() == 0) {
          Response.sendFailure(routingContext, 404, USER_NOT_FOUND_ERROR);
        } else {
          Row row = rows.iterator().next();
          String storedHash = row.getString("password");

          // Check if password is correct
          if (Password.check(password, storedHash).withArgon2()) {

            // Create session
            Session session = routingContext.session();
            session.put("id", row.getInteger("id").toString());

            Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", LOGIN_SUCCESSFUL));

            // Update last action on database
            pool.preparedQuery("UPDATE users SET last_action = NOW() WHERE id = ?")
              .execute(Tuple.of(row.getInteger("id")));

          } else {
            Response.sendFailure(routingContext, 400, LOGIN_FAIL_ERROR);
          }
        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  public void getChallenges(RoutingContext routingContext, MySQLPool pool) {
    String id = routingContext.session().get("id");
    if (id == null) {
      Response.sendFailure(routingContext, 401, NOT_LOGGED_IN_ERROR);
      return;
    }

    pool.preparedQuery("SELECT * FROM solved_challenges WHERE user_id = ?")
      .execute(Tuple.of(id))
      .onSuccess(solvedChallenges -> pool.preparedQuery("SELECT * FROM challenges")
        .execute()
        .onSuccess(challenges -> {

          // Update last action on database
          pool.preparedQuery("UPDATE users SET last_action = NOW() WHERE id = ?")
            .execute(Tuple.of(id));


          JsonObject response = new JsonObject();
          for (Row challenge : challenges) {
            response.put(challenge.getInteger("challenge_id").toString(), "unsolved");
          }

          for (Row solvedChallenge : solvedChallenges) {
            response.put(solvedChallenge.getInteger("solved_challenge_id").toString(), "solved");
          }

          Response.sendJsonResponse(routingContext, 200, response);
        })).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  public void submitChallenge(RoutingContext routingContext, MySQLPool pool, String challengeId, String flag) {

    String id = routingContext.session().get("id");
    if (id == null) {
      Response.sendFailure(routingContext, 401, NOT_LOGGED_IN_ERROR);
      return;
    }

    pool.preparedQuery(SQL_SELECT_USER_BY_ID)
      .execute(Tuple.of(id))
      .onSuccess(result -> {
        if (result.size() == 0) {

          Response.sendFailure(routingContext, 404, USER_SESSION_NOT_FOUND_ERROR);

        } else if (result.iterator().next().getBoolean("disabled")) {

          Response.sendFailure(routingContext, 409, USER_DISABLED_ERROR);

        } else {
          // Update last action on database
          pool.preparedQuery("UPDATE users SET last_action = NOW() WHERE id = ?")
            .execute(Tuple.of(id));

          pool.preparedQuery("SELECT * FROM challenges WHERE challenge_id = ? AND flag = ?")
            .execute(Tuple.of(challengeId, flag))
            .onSuccess(rows -> {
              if (rows.size() == 0) {

                Response.sendFailure(routingContext, 404, "wrong flag");

              } else {

                // check if challenge already solved
                pool.preparedQuery("SELECT * FROM solved_challenges WHERE user_id = ? AND solved_challenge_id = ?")
                  .execute(Tuple.of(id, challengeId))
                  .onSuccess(solvedChallenges -> {
                    if (solvedChallenges.size() != 0) {
                      Response.sendFailure(routingContext, 400, "challenge already solved");
                    } else {
                      pool.preparedQuery("INSERT INTO solved_challenges (user_id, solved_challenge_id) VALUES (?, ?)")
                        .execute(Tuple.of(id, challengeId))
                        .onSuccess(res -> Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "challenge solved")));
                    }
                  });
              }
            });

        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  public void toggleUser(RoutingContext routingContext, MySQLPool pool, String userIdToBeToggled) {
    String requestingUsersId = routingContext.session().get("id");
    if (requestingUsersId == null) {
      Response.sendFailure(routingContext, 401, NOT_LOGGED_IN_ERROR);
      return;
    }

    pool.preparedQuery(SQL_SELECT_USER_BY_ID)
      .execute(Tuple.of(requestingUsersId))
      .onSuccess(result -> {
        if (result.size() == 0) {

          Response.sendFailure(routingContext, 404, USER_SESSION_NOT_FOUND_ERROR);

        } else if (result.iterator().next().getBoolean("disabled")) {

          Response.sendFailure(routingContext, 403, USER_DISABLED_ERROR);

        } else if (!result.iterator().next().getBoolean("administrator")) {

          Response.sendFailure(routingContext, 403, USER_NOT_ADMIN_ERROR);

        } else {

          // Update last action on database for the admin that made the toggle request
          pool.preparedQuery("UPDATE users SET last_action = NOW() WHERE id = ?")
            .execute(Tuple.of(requestingUsersId));

          pool.preparedQuery(SQL_SELECT_USER_BY_ID)
            .execute(Tuple.of(userIdToBeToggled))
            .onSuccess(user -> {
              if (user.size() == 0) {

                Response.sendFailure(routingContext, 404, USER_NOT_FOUND_ERROR);

              } else {
                pool.preparedQuery("UPDATE users SET disabled = ? WHERE id = ?")
                  .execute(Tuple.of(!user.iterator().next().getBoolean("disabled"), userIdToBeToggled))
                  .onSuccess(res -> {
                    if (user.iterator().next().getBoolean("disabled")) {
                      Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "user enabled"));
                    } else {
                      Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "user disabled"));
                    }
                  });
              }
            });
        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  public void getOnlineUsers(RoutingContext routingContext, MySQLPool pool) {
    String requestingUsersId = routingContext.session().get("id");
    if (requestingUsersId == null) {
      Response.sendFailure(routingContext, 401, NOT_LOGGED_IN_ERROR);
      return;
    }

    pool.preparedQuery(SQL_SELECT_USER_BY_ID)
      .execute(Tuple.of(requestingUsersId))
      .onSuccess(result -> {
        if (result.size() == 0) {

          Response.sendFailure(routingContext, 404, USER_SESSION_NOT_FOUND_ERROR);

        } else if (result.iterator().next().getBoolean("disabled")) {

          Response.sendFailure(routingContext, 403, USER_DISABLED_ERROR);

        } else if (!result.iterator().next().getBoolean("administrator")) {

          Response.sendFailure(routingContext, 403, USER_NOT_ADMIN_ERROR);

        } else {
          // Get all users where last action was less than 30 minutes ago
          pool.preparedQuery("SELECT * FROM users WHERE last_action > NOW() - INTERVAL 30 MINUTE")
            .execute()
            .onSuccess(onlineUsers -> {
              JsonObject response = new JsonObject();
              for (Row row : onlineUsers) {
                response.put(row.getInteger("id").toString(), new JsonObject()
                  .put("username", row.getString("username"))
                  .put("disabled", row.getBoolean("disabled"))
                  .put("admin", row.getBoolean("administrator")));
              }
              Response.sendJsonResponse(routingContext, 200, response);
            });
        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  public void getUser(RoutingContext routingContext, MySQLPool pool) {
    String userId = routingContext.session().get("id");

    if (userId == null) {
      Response.sendFailure(routingContext, 401, NOT_LOGGED_IN_ERROR);
      return;
    }

    // Get user and from database
    pool.preparedQuery(SQL_SELECT_USER_BY_ID)
      .execute(Tuple.of(userId))
      .onSuccess(userDetails -> {
        if (userDetails.size() == 0) {

          Response.sendFailure(routingContext, 404, USER_NOT_FOUND_ERROR);

        } else {
          Response.sendJsonResponse(routingContext, 200, new JsonObject()
            .put("username", userDetails.iterator().next().getString("username"))
            .put("img_id", userDetails.iterator().next().getString("img_id"))
            .put("disabled", userDetails.iterator().next().getBoolean("disabled"))
            .put("admin", userDetails.iterator().next().getBoolean("administrator")));
        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));

  }

  public void uploadImg(RoutingContext routingContext, MySQLPool pool, List<FileUpload> files) {
    // Check if user is logged in
    String userId = routingContext.session().get("id");

    if (userId == null) {
      Response.sendFailure(routingContext, 401, NOT_LOGGED_IN_ERROR);
      deleteFiles(files);
      return;
    }

    // Validate user
    pool.preparedQuery(SQL_SELECT_USER_BY_ID)
      .execute(Tuple.of(userId))
      .onSuccess(userDetails -> {
        if (userDetails.size() == 0) {

          Response.sendFailure(routingContext, 404, USER_NOT_FOUND_ERROR);
          deleteFiles(files);

        } else if (userDetails.iterator().next().getBoolean("disabled")) {

          Response.sendFailure(routingContext, 403, USER_DISABLED_ERROR);
          deleteFiles(files);

        } else if (files.size() > 1) {

          Response.sendFailure(routingContext, 400, "Only one file can be uploaded at a time");
          deleteFiles(files);

        } else if (files.isEmpty()) {

          Response.sendFailure(routingContext, 400, "No file was uploaded");
          deleteFiles(files);

        } else {
          // Get file
          FileUpload file = files.get(0);

          // Check if file is an image
          if (!file.contentType().startsWith("image/")) {
            Response.sendFailure(routingContext, 400, "File is not an image");
            deleteFiles(files);
            return;
          }

          // Get file extension
          String extension = file.fileName().substring(file.fileName().lastIndexOf(".") + 1);

          // Generate random file name
          String fileName = UUID.randomUUID().toString() + "." + extension;

          // Create uploads folder if it doesn't exist
          if (!new File("uploads/images/").exists()) {
            new File("uploads/images").mkdirs();
          }

          // Move file to uploads folder
          new File(file.uploadedFileName()).renameTo(new File("uploads/images/" + fileName));

          // Delete old image
          String oldImgId = userDetails.iterator().next().getString("img_id");
          if (oldImgId != null) {
            if (new File("uploads/images/"+ oldImgId).exists()) {
              try {
                Files.delete(Paths.get("uploads/images/" + oldImgId));
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }

          // Update database with new image id
          pool.preparedQuery("UPDATE users SET img_id = ? WHERE id = ?")
            .execute(Tuple.of(fileName, userId))
            .onSuccess(res -> Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "image uploaded")));
        }
      }).onFailure(err -> Response.sendFailure(routingContext, 500, err.getMessage()));
  }

  private void deleteFiles(List<FileUpload> files) {
    for (FileUpload file : files) {
      new File(file.uploadedFileName()).delete();
    }
  }
}
