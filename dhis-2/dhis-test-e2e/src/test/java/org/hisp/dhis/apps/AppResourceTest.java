/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.apps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Tag("apptests")
class AppResourceTest extends ApiTest {

  private static final RestTemplate restTemplate =
      new RestTemplate(
          new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) {
              connection.setInstanceFollowRedirects(false);
            }
          });
  private static final String SERVER_BASE = "http://web:8080";

  @Test
  @DisplayName("Redirect location should have correct format")
  void redirectLocationCorrectFormatTest() {
    // given an app is installed
    File file = new File("src/test/resources/apps/test-app-v1.zip");
    given()
        .multiPart("file", file)
        .contentType("multipart/form-data")
        .when()
        .post("/apps")
        .then()
        .statusCode(201);

    // when called with missing trailing slash
    ApiResponse response =
        new ApiResponse(given().redirects().follow(false).get("/apps/test-minio"));

    // then redirect should be returned with trailing slash
    response.validate().header("location", equalTo("http://web:8080/api/apps/test-minio/"));
    response.validate().statusCode(302);
  }

  @ParameterizedTest
  @DisplayName("Bundled apps are served from /dhis-web-<app> paths with correct internal redirects")
  @ValueSource(
      strings = {"dashboard", "maintenance", "maps", "capture", "settings", "app-management"})
  void bundledAppServedFromDhisWebPath(String app) {

    // Serve index.html from index.html?redirect=false
    {
      ResponseEntity<String> response =
          getAuthenticated("/dhis-web-" + app + "/index.html?redirect=false");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      // TODO: Confirm content-type and template replacement
    }

    // Serve index.html from /?redirect=false
    {
      ResponseEntity<String> response = getAuthenticated("/dhis-web-" + app + "/?redirect=false");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
    }

    // Append trailing slash and redirect
    {
      ResponseEntity<String> response = getAuthenticated("/dhis-web-" + app);
      assertEquals(HttpStatus.FOUND, response.getStatusCode());
      List<String> location = response.getHeaders().get("Location");
      assertEquals(SERVER_BASE + "/dhis-web-" + app + "/", location.get(0));
    }

    // Serve index.html from index.html (non-navigation)
    {
      ResponseEntity<String> response = getAuthenticated("/dhis-web-" + app + "/index.html");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
    }

    // Serve index.html from / (non-navigation)
    {
      ResponseEntity<String> response = getAuthenticated("/dhis-web-" + app + "/");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
    }

    // Redirect to global shell from index.html (navigation)
    // TODO: Set Sec-Fetch-Mode=navigate
    // {
    //   ResponseEntity<String> response = getAuthenticated("/dhis-web-" + app + "/index.html");
    //   assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //   List<String> location = response.getHeaders().get("Location");
    //   assertEquals(SERVER_BASE + "/apps/" + app, location.get(0));
    // }

    // Redirect to global shell from / (navigation)
    // TODO: Set Sec-Fetch-Mode=navigate
    // {
    //   ResponseEntity<String> response aa = getAuthenticated("/dhis-web-" + app + "/");
    //   assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //   List<String> location = response.getHeaders().get("Location");
    //   assertEquals(SERVER_BASE + "/apps/" + app, location.get(0));
    // }

    // Redirect index.action
    // TODO: Fix index.action redirects
    // {
    //   ResponseEntity<String> response = getAuthenticated("/dhis-web-" + app + "/index.action");
    //   assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //   List<String> location = response.getHeaders().get("Location");
    //   assertEquals(SERVER_BASE + "/dhis-web-" + app + "/index.html", location.get(0));
    // }
  }

  // TODO: Installed apps
  // TODO: Global shell
  // TODO: Test when global shell disabled

  private ResponseEntity<String> get(String path, HttpHeaders headers) {
    try {
      return restTemplate.exchange(
          SERVER_BASE + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  private ResponseEntity<String> get(String path) {
    return get(path, new HttpHeaders());
  }

  private ResponseEntity<String> getAuthenticated(String path) {
    // TODO: Use cookies and test with multiple users
    String authHeader =
        Base64.getUrlEncoder().encodeToString("admin:district".getBytes(StandardCharsets.UTF_8));
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Basic " + authHeader);
    return get(path, headers);
  }
}
