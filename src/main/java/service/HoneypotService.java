package service;

import api.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;

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
        response.put("error", err.getMessage());
        Response.sendJsonResponse(ctx, 500, response);
      });
  }
}
