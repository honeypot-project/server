package api;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

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
    Request request = Request.from(routingContext);
    String input = request.getTestRequestParams();
    Response.sendJsonResponse(routingContext, 200, new JsonObject().put("you said", input));
  }

  public static void addImage(RoutingContext routingContext) {
    System.out.println("adding");
    HttpServerRequest request = routingContext.request();
    String type = request.getFormAttribute("type");

    List<FileUpload> files = routingContext.fileUploads();
    System.out.println(files.size());
    for (FileUpload file : files) {
      System.out.println(file.fileName());
    }

    Response.sendOkResponse(routingContext);
  }
}
