package service;

import api.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class HoneypotService {
  public void getUsers(RoutingContext routingContext, MySQLPool pool) {
    JsonObject response = new JsonObject();
    String id = routingContext.session().get("id");

    if (id == null) {
      Response.sendFailure(routingContext, 401, "You are not logged in");
      return;
    }

    pool.preparedQuery("SELECT * FROM users WHERE id = ?")
        .execute(Tuple.of(id))
          .onSuccess(result -> {
            if (result.size() == 0) {

              Response.sendFailure(routingContext, 404, "Please login again");

            } else if (result.iterator().next().getBoolean("disabled")) {

              Response.sendFailure(routingContext, 403, "User is disabled");

            } else if (result.iterator().next().getBoolean("administrator")) {

              Response.sendFailure(routingContext, 403, "You are not an administrator");

            } else {
                pool.query("SELECT * FROM users")
                  .execute()
                  .onSuccess(rows -> {
                    for (Row row : rows) {
                      response.put(row.getInteger("id").toString(), new JsonObject()
                        .put("username", row.getString("username"))
                        .put("disabled", row.getBoolean("disabled"))
                        .put("admin", row.getBoolean("administrator")));
                    }
                    Response.sendJsonResponse(routingContext, 200, response);
                  });
                }
              }).onFailure(err -> {
                Response.sendFailure(routingContext, 500, err.getMessage());
      });
  }

  public void addUser(RoutingContext routingContext, MySQLPool pool, String username, String password) {
    pool.preparedQuery("SELECT * FROM users WHERE username = ?")
      .execute(Tuple.of(username))
      .onSuccess(rows -> {
        if (rows.size() != 0) {
          System.out.println("User already exists");
          Response.sendJsonResponse(routingContext, 400, new JsonObject().put("error", "User already exists"));
          return;
        } else {
          pool.preparedQuery("INSERT INTO users (username, password) VALUES (?, ?)")
            .execute(Tuple.of(username, password))
            .onSuccess(res -> {
              Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "User added"));
            });
        }
      }).onFailure(err -> {
          Response.sendFailure(routingContext, 500, err.getMessage());
      });
  }

  public void login(RoutingContext routingContext, MySQLPool pool, String username, String password) {
    pool.preparedQuery("SELECT * FROM users WHERE username = ? AND password = ?")
      .execute(Tuple.of(username, password))
      .onSuccess(rows -> {
        if (rows.size() == 0) {
          Response.sendJsonResponse(routingContext, 400, new JsonObject().put("error", "User not found"));
          return;
        } else {
          Session session = routingContext.session();
          session.put("id", rows.iterator().next().getInteger("id").toString());
          Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "Login successful"));
        }
      }).onFailure(err -> {
        Response.sendFailure(routingContext, 500, err.getMessage());
      });
  }

  public void getChallenges(RoutingContext routingContext, MySQLPool pool) {
    String id = routingContext.session().get("id");
    if (id == null) {
      Response.sendJsonResponse(routingContext, 401, new JsonObject().put("error", "Not logged in"));
      return;
    }

    pool.preparedQuery("SELECT * FROM solved_challenges WHERE user_id = ?")
      .execute(Tuple.of(id))
      .onSuccess(solvedChallenges -> {
        JsonObject response = new JsonObject();
        pool.preparedQuery("SELECT * FROM challenges")
          .execute()
          .onSuccess(challenges -> {
            for (Row challenge : challenges) {
              response.put(challenge.getInteger("challenge_id").toString(), "unsolved");
            }

            System.out.println(response);

            for (Row solvedChallenge : solvedChallenges) {
              response.put(solvedChallenge.getInteger("solved_challenge_id").toString(), "solved");
            }

            Response.sendJsonResponse(routingContext, 200, response);
          });
      }).onFailure(err -> {
        Response.sendFailure(routingContext, 500, err.getMessage());
      });
  }

  public void submitChallenge(RoutingContext routingContext, MySQLPool pool, String challengeId, String flag) {
    String id = routingContext.session().get("id");
    if (id == null) {
      Response.sendJsonResponse(routingContext, 401, new JsonObject().put("error", "Not logged in"));
      return;
    }

    pool.preparedQuery("SELECT * FROM challenges WHERE challenge_id = ? AND flag = ?")
      .execute(Tuple.of(challengeId, flag))
      .onSuccess(rows -> {
        if (rows.size() == 0) {
          Response.sendJsonResponse(routingContext, 400, new JsonObject().put("error", "Wrong flag"));
          return;
        }

        pool.preparedQuery("INSERT INTO solved_challenges (user_id, solved_challenge_id) VALUES (?, ?)")
          .execute(Tuple.of(id, challengeId))
          .onSuccess(res -> {
            Response.sendJsonResponse(routingContext, 200, new JsonObject().put("ok", "Challenge solved"));
          });

      }).onFailure(err -> {
        Response.sendFailure(routingContext, 500, err.getMessage());
      });
  }
}
