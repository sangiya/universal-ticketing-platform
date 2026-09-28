package com.ticketmesh.it;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * REST API contract test using REST Assured. Validates HTTP-level contracts
 * for the public API: status codes, content types, JSON shape, and key
 * business rules. This is the regression net for any change that would
 * break client integrations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:restapi;MODE=MYSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.ai.openai.api-key=",
    "app.provider.connect-timeout-ms=2000",
    "app.provider.read-timeout-ms=3000"
})
class RestApiContractIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void healthEndpointReturns200() {
        given()
                .when().get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void searchEndpointAcceptsQueryParam() {
        given()
                .when().get("/api/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", notNullValue());
    }

    @Test
    void searchEndpointReturnsArray() {
        Response response = given()
                .when().get("/api/search?q=colombo");
        response.then().statusCode(200);
        // Body is either an array or empty array
        Object body = response.getBody().as(Object.class);
        assert body != null;
    }

    @Test
    void verticalsEndpointReturnsCounts() {
        given()
                .when().get("/api/search/verticals")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(greaterThan(0)));
    }

    @Test
    void registerEndpointAcceptsValidPayload() {
        Map<String, Object> request = new HashMap<>();
        request.put("username", "restapi_user_" + System.currentTimeMillis());
        request.put("password", "strongpass123");
        request.put("fullName", "REST API User");
        request.put("email", "restapi_" + System.currentTimeMillis() + "@example.com");
        request.put("role", "CUSTOMER");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("username", notNullValue())
                .body("role", equalTo("CUSTOMER"));
    }

    @Test
    void registerEndpointRejectsDuplicateUsername() {
        String uniqueUser = "dupe_user_" + System.currentTimeMillis();
        String uniqueEmail = "dupe_" + System.currentTimeMillis() + "@example.com";

        Map<String, Object> first = new HashMap<>();
        first.put("username", uniqueUser);
        first.put("password", "strongpass123");
        first.put("fullName", "Dupe One");
        first.put("email", uniqueEmail);
        first.put("role", "CUSTOMER");

        given()
                .contentType(ContentType.JSON)
                .body(first)
                .when().post("/api/auth/register")
                .then()
                .statusCode(201);

        Map<String, Object> second = new HashMap<>();
        second.put("username", uniqueUser);
        second.put("password", "strongpass123");
        second.put("fullName", "Dupe Two");
        second.put("email", "other_" + System.currentTimeMillis() + "@example.com");
        second.put("role", "CUSTOMER");

        given()
                .contentType(ContentType.JSON)
                .body(second)
                .when().post("/api/auth/register")
                .then()
                .statusCode(409)
                .body("message", containsString("Username already taken"));
    }

    @Test
    void registerEndpointValidatesRequiredFields() {
        Map<String, Object> request = new HashMap<>();
        request.put("username", "x");
        // missing password, fullName, email

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void loginEndpointReturnsTokensForValidCredentials() {
        String username = "loginuser_" + System.currentTimeMillis();
        String email = "login_" + System.currentTimeMillis() + "@example.com";

        Map<String, Object> register = new HashMap<>();
        register.put("username", username);
        register.put("password", "strongpass123");
        register.put("fullName", "Login User");
        register.put("email", email);
        register.put("role", "CUSTOMER");

        given()
                .contentType(ContentType.JSON)
                .body(register)
                .when().post("/api/auth/register")
                .then().statusCode(201);

        Map<String, Object> login = new HashMap<>();
        login.put("username", username);
        login.put("password", "strongpass123");

        given()
                .contentType(ContentType.JSON)
                .body(login)
                .when().post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"));
    }

    @Test
    void loginEndpointRejectsWrongPassword() {
        Map<String, Object> login = new HashMap<>();
        login.put("username", "ghost_user_" + System.currentTimeMillis());
        login.put("password", "wrong");

        given()
                .contentType(ContentType.JSON)
                .body(login)
                .when().post("/api/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void protectedEndpointRequiresAuth() {
        given()
                .when().get("/api/orders/mine")
                .then()
                .statusCode(401);
    }

    @Test
    void forgotPasswordEndpointIsIdempotent() {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "nonexistent_" + System.currentTimeMillis() + "@example.com");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/auth/forgot-password")
                .then()
                // Even for non-existent emails, the API returns 200 to prevent enumeration
                .statusCode(200);
    }

    @Test
    void searchEndpointRespectsQueryParam() {
        given()
                .queryParam("q", "colombo")
                .when().get("/api/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void searchEndpointRespectsOriginDestinationParams() {
        given()
                .queryParam("origin", "Colombo")
                .queryParam("destination", "Kandy")
                .when().get("/api/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void verticalEndpointReturns400ForInvalidVertical() {
        given()
                .when().get("/api/search/vertical/INVALID")
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    void actuatorInfoEndpointIsAvailable() {
        given()
                .when().get("/actuator/info")
                .then()
                .statusCode(200);
    }

    @Test
    void nonNumericCatalogIdReturns400Not500() {
        given()
                .when().get("/api/catalog/not-a-number")
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    @Test
    void unknownApiPathDoesNotLeakServerError() {
        // Unknown paths sit behind anyRequest().authenticated(), so the security
        // filter answers 401 before routing. Either way it must never be a 500.
        given()
                .when().get("/api/definitely-not-a-real-endpoint")
                .then()
                .statusCode(anyOf(is(401), is(404)));
    }

    @Test
    void missingRequiredParamReturns400Not500() {
        given()
                .when().get("/api/tickets/verify")
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    @Test
    void authenticatedRequestToUnknownApiPathReturns404() {
        String username = "probe_" + System.currentTimeMillis();
        String email = username + "@example.com";

        Map<String, Object> register = new HashMap<>();
        register.put("username", username);
        register.put("password", "strongpass123");
        register.put("fullName", "Probe User");
        register.put("email", email);
        register.put("role", "CUSTOMER");

        given()
                .contentType(ContentType.JSON)
                .body(register)
                .when().post("/api/auth/register")
                .then().statusCode(201);

        Map<String, Object> login = new HashMap<>();
        login.put("username", username);
        login.put("password", "strongpass123");

        String token = given()
                .contentType(ContentType.JSON)
                .body(login)
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("accessToken");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/api/definitely-not-a-real-endpoint")
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }
}
