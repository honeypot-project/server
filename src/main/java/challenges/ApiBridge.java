package challenges;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ApiBridge {


  public static void hello(RoutingContext routingContext) {
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("message", "ok"));
  }

  public static void testBody(RoutingContext routingContext) {
    Request request = Request.from(routingContext);
    String input = request.getTestBody();
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("you said", input));
  }

  public static void testPath(RoutingContext routingContext) {
    System.out.println(routingContext);
    System.out.println(routingContext.body().asJsonObject());

    Request request = Request.from(routingContext);
    String input = request.getTestRequestParams();
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("you said", input));
  }
}
