package gatlingdemostore;

import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class GatlingDemoStoreProductApis extends Simulation {

  private static HttpProtocolBuilder httpProtocol;
  private static Map<CharSequence, String> authorization;

  {
    httpProtocol = http
      .baseUrl("https://demostore.gatling.io")
      .header("Cache-Control", "no-cache")
      .contentTypeHeader("application/json")
      .acceptHeader("application/json");
    
    authorization = new HashMap<>();
    authorization.put("authorization", "Bearer #{jwt}");

    class Authentication {
      private static ChainBuilder createToken =
              exec(http("Create Authentication Token")
                      .post("/api/authenticate")
                      .body(RawFileBody("gatlingdemostore/gatlingdemostoreauthenticationapi/user_credentials_request.json"))
                      .check(status().is(200))
                      .check(jmesPath("token").saveAs("jwt")));
    }

    class Products {
      private static ChainBuilder getAllProducts = exec(http("List All Products")
              .get("/api/product")
              .check(status().is(200)));

      private static ChainBuilder getAllProductsByCategory(String categoryId) {
        return exec(http("List All Products By Given Category")
                .get("/api/product?category="+categoryId)
                .check(status().is(200))
                .check(jmesPath("[? categoryId != "+"'"+categoryId+"'"+"]").ofList().is(Collections.emptyList())));
      }

      private static ChainBuilder getProductById(String productId, String expected) {
        return exec(http("Get Particular Product")
               .get("/api/product/"+productId)
               .check(status().is(200))
               .check(jmesPath("name").is(expected)));
      }

      private static ChainBuilder createNewProduct = exec( http("Create New Product")
              .post("/api/product")
              .headers(authorization)
              .body(RawFileBody("gatlingdemostore/gatlingdemostoreproductapis/create_new_product_request.json"))
              .check(status().is(200))
              .check(jmesPath("name").is("My new product")));

      private static ChainBuilder updateExistingProduct = exec( http("Update Existing Product")
              .put("/api/product/17")
              .headers(authorization)
              .body(RawFileBody("gatlingdemostore/gatlingdemostoreproductapis/update_existing_product_request.json"))
              .check(status().is(200))
              .check(jmesPath("id").ofInt().is(17))
              .check(jmesPath("name").is("My updated product")));

    }

    ScenarioBuilder scn = scenario("GatlingDemoStoreProductApis")
      .exec(Products.getAllProducts)
      .pause(2)
      .exec(Products.getAllProductsByCategory("7"))
      .pause(2)
      .exec(Products.getProductById("17", "Casual Black-Blue"))
      .pause(2)
      .exec(Authentication.createToken)
      .pause(2)
      .exec(Products.createNewProduct)
      .pause(2)
      .exec(Products.updateExistingProduct);

	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
