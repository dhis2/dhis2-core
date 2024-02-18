package org.hisp.dhis.auth;


import static org.hisp.dhis.system.StartupEventPublisher.SERVER_STARTED_LATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.system.util.HttpHeadersBuilder;
import org.hisp.dhis.web.embeddedjetty.JettyEmbeddedCoreWeb;
import org.hisp.dhis.webapi.controller.security.LoginRequest;
import org.hisp.dhis.webapi.controller.security.LoginResponse;
import org.hisp.dhis.webapi.controller.user.MeDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
class AuthTest {
  private static final String POSTGRES_POSTGIS_VERSION = "10-2.5-alpine";
  private static final DockerImageName POSTGIS_IMAGE_NAME =
      DockerImageName.parse("postgis/postgis").asCompatibleSubstituteFor("postgres");
  private static final String POSTGRES_DATABASE_NAME = "dhis";
  private static final String POSTGRES_USERNAME = "dhis";
  private static final String POSTGRES_PASSWORD = "dhis";
  private static PostgreSQLContainer<?> POSTGRES_CONTAINER;

  @BeforeAll
  static void setup() throws Exception {

    POSTGRES_CONTAINER =
        new PostgreSQLContainer<>(POSTGIS_IMAGE_NAME.withTag(POSTGRES_POSTGIS_VERSION))
            .withDatabaseName(POSTGRES_DATABASE_NAME)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD)
            .withInitScript("db/extensions.sql")
            .withTmpFs(Map.of("/testtmpfs", "rw"))
            .withEnv("LC_COLLATE", "C");

    POSTGRES_CONTAINER.start();

    createTmpDhisConf();

    System.setProperty("dhis2.home", System.getProperty("java.io.tmpdir"));

    Thread printingHook = new Thread(() -> {
      System.out.println("In the middle of a shutdown");
    });
    Runtime.getRuntime().addShutdownHook(printingHook);

    Thread longRunningHook = new Thread(() -> {
      try {
        JettyEmbeddedCoreWeb.main(null);
      } catch (InterruptedException ignored) {
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    longRunningHook.start();

    SERVER_STARTED_LATCH.await();

    log.error("Server started");
  }

  private static void createTmpDhisConf() {
    String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
    log.error("JDBC URL: " + jdbcUrl);
    String multiLineString = """
        connection.dialect = org.hibernate.dialect.PostgreSQLDialect
        connection.driver_class = org.postgresql.Driver
        connection.url = %s
        connection.username = dhis
        connection.password = dhis
        # Database schema behavior, can be validate, update, create, create-drop
        connection.schema = update
        system.audit.enabled = false
        """.formatted(jdbcUrl);
    try {
      String tmpDir = System.getProperty("java.io.tmpdir");
      Path tmpFilePath = Path.of(tmpDir, "dhis.conf");
      Files.writeString(tmpFilePath, multiLineString, StandardOpenOption.CREATE);
      System.out.println("File written successfully to " + tmpFilePath);
    } catch (Exception e) {
      log.error("Error creating file", e);
    }
  }


  @Test
  void testLogin() {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeadersBuilder headersBuilder = new HttpHeadersBuilder().withContentTypeJson();

    LoginRequest loginRequest = LoginRequest.builder().username("admin").password("district")
        .build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, headersBuilder.build());

    ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
        "http://localhost:9090/api/auth/login", requestEntity,
        LoginResponse.class);

    assertNotNull(loginResponse);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    LoginResponse body = loginResponse.getBody();
    assertNotNull(body);
    assertEquals(LoginResponse.STATUS.SUCCESS, body.getLoginStatus());
    HttpHeaders headers = loginResponse.getHeaders();

    log.error("Headers: " + headers);

    assertEquals("/dhis-web-dashboard", body.getRedirectUrl());

    assertNotNull(headers);
    List<String> cookieHeader = headers.get(HttpHeaders.SET_COOKIE);
    assertNotNull(cookieHeader);
    assertEquals(1, cookieHeader.size());
    String cookie = cookieHeader.get(0);

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set("Cookie", cookie);
    HttpEntity<String> getEntity = new HttpEntity<>("", getHeaders);

    ResponseEntity<JsonNode> getResponse = restTemplate.exchange("http://localhost:9090/api/me",
        HttpMethod.GET, getEntity, JsonNode.class);

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());

    assertNotNull(getResponse);
    assertNotNull(getResponse.getBody());
    JsonNode body1 = getResponse.getBody();
    log.error("MeDto: " + body1);
  }
}
