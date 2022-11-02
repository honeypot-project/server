package service;

import api.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class HoneypotService {
  public void getUsers(RoutingContext ctx, MySQLPool pool) {
    JsonObject response = new JsonObject();
    pool.query("SELECT * FROM users")
      .execute()
      .onSuccess(rows -> {{
        System.out.println("Got " + rows.size() + " rows ");

          for (Row row : rows) {
            response.put(row.getInteger("id").toString(), row.getString("username"));
          }

          Response.sendJsonResponse(ctx, 200, response);
        }
      }).onFailure(err -> {
        System.out.println("Failure: " + err.getMessage());
        Response.sendFailure(ctx, 500, err.getMessage());
      });
  }

  public void addUser(RoutingContext routingContext, MySQLPool pool, String username, String password) {
    pool.preparedQuery("SELECT * FROM users WHERE username = ?").execute(Tuple.of(username))
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
}
