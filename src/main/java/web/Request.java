package web;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;



/**
 * The Request class is responsible for translating information that is part of the
 * request into Java.
 * <p>
 * For every piece of information that you need from the request, you should provide a method here.
 * You can find information in:
 * - the request path: params.pathParameter("some-param-name")
 * - the query-string: params.queryParameter("some-param-name")
 * Both return a `RequestParameter`, which can contain a string or an integer in our case.
 * The actual data can be retrieved using `getInteger()` or `getString()`, respectively.
 * You can check if it is an integer (or not) using `isNumber()`.
 * <p>
 * Finally, some requests have a body. If present, the body will always be in the json format.
 * You can access this body using: `params.body().getJsonObject()`.
 * <p>
 * **TIP:** Make sure that al your methods have a unique name. For instance, there is a request
 * that consists of more than one "player name". You cannot use the method `getPlayerName()` for both,
 * you will need a second one with a different name.
 */
public class Request {

  private final RoutingContext ctx;
  private final MultiMap params;
  private final RequestBody body;

  private Request(RoutingContext ctx) {
    this.ctx = ctx;
    this.params = ctx.queryParams();
    this.body = ctx.body();
  }

  public static Request from(RoutingContext ctx) {
    return new Request(ctx);
  }

  public String getTestRequestParams() {
    return params.get("test");
  }

  public String getTestBody() {
    return body.asJsonObject().getString("test");
  }

  public String getUsername() {
    return body.asJsonObject().getString("username");
  }

  public String getPassword() {
    return body.asJsonObject().getString("password");
  }

  public String getChallengeId() {
    return body.asJsonObject().getString("id");
  }

  public String getFlag() {
    return body.asJsonObject().getString("flag");
  }

  public int getUserId() {
    return Integer.parseInt(params.get("user"));
  }
}
