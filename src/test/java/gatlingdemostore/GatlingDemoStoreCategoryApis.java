package gatlingdemostore;

import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class GatlingDemoStoreCategoryApis extends Simulation {

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
                       .check(jsonPath("$.token").saveAs("jwt")));
    }

    class Category {
      private static ChainBuilder getAllCategory =
              exec(http("List All Categories")
                      .get("/api/category")
                      .check(status().is(200))
                      .check(jsonPath("$..[?(@.id == 6)].name").is("For Her")));
      private static ChainBuilder getParticularCategory(String categoryId) {
        return exec(http("Get Particular Category")
                       .get("/api/category/"+categoryId)
                       .check(status().is(200))
                       .check(jsonPath("$.name").is("Unisex"))
        );
      }

      private static ChainBuilder createNewCategory =
              exec(http("Create New Category")
                      .post("/api/category")
                      .headers(authorization)
                      .body(RawFileBody("gatlingdemostore/gatlingdemostorecategoryapis/create_new_category_request.json"))
                      .check(status().is(200))
                      .check(jsonPath("$.name").is("Alien")));
      private static ChainBuilder updateExistingCategory(String categoryId) {
        return exec(http("Update Existing Category")
                .put("/api/category/"+categoryId)
                .headers(authorization)
                .body(RawFileBody("gatlingdemostore/gatlingdemostorecategoryapis/update_existing_category_request.json"))
                .check(status().is(200))
                .check(jsonPath("$.id").ofInt().is(Integer.valueOf(categoryId)))
                .check(jsonPath("$.name").is("Everyone")));
      }

    }

    ScenarioBuilder scn = scenario("GatlingDemoStoreApis")
      .exec(Category.getAllCategory)
      .pause(2)
      .exec(Category.getParticularCategory("7"))
      .pause(2)
      .exec(Authentication.createToken)
      .pause(2)
      .exec(Category.createNewCategory)
      .pause(2)
      .exec(Category.updateExistingCategory("7"));

	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }

}